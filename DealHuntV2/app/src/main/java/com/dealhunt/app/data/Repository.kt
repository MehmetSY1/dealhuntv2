package com.dealhunt.app.data

import com.dealhunt.app.model.*
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.net.URL
import java.util.concurrent.TimeUnit

// ── CheapShark API ──────────────────────────────────────────────────────────
interface CheapSharkApi {
    @GET("games")
    suspend fun searchGames(@Query("title") title: String, @Query("limit") limit: Int = 60): List<GameSearchResult>

    @GET("games")
    suspend fun getGameInfo(@Query("id") gameId: String): GameInfo

    @GET("deals")
    suspend fun getDeals(
        @Query("storeID") storeIds: String = "1,25,7,11,15,23,24,28,8,21,13",
        @Query("pageSize") pageSize: Int = 30,
        @Query("sortBy") sortBy: String = "Savings",
        @Query("onSale") onSale: Int = 1
    ): List<DealDetail>

    @GET("stores")
    suspend fun getStores(): List<Store>
}

// ── Network Client ──────────────────────────────────────────────────────────
object NetworkClient {
    private const val BASE = "https://www.cheapshark.com/api/1.0/"
    private const val CDN  = "https://www.cheapshark.com"

    val api: CheapSharkApi = Retrofit.Builder()
        .baseUrl(BASE)
        .client(OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(CheapSharkApi::class.java)

    fun logoUrl(path: String) = "$CDN$path"
    fun dealUrl(id: String) = "https://www.cheapshark.com/redirect?dealID=$id"
}

// ── Exchange Rate Manager ────────────────────────────────────────────────────
object ExchangeRateManager {
    private var cachedRate: Double = 0.0
    private var lastFetchTime: Long = 0
    private const val CACHE_DURATION = 30 * 60 * 1000L // 30 dakika

    // Birden fazla API dene — biri çalışmazsa diğerine geç
    suspend fun getUsdToTry(): Double {
        val now = System.currentTimeMillis()
        if (cachedRate > 0 && (now - lastFetchTime) < CACHE_DURATION) {
            return cachedRate
        }

        val rate = tryApi1() ?: tryApi2() ?: tryApi3() ?: 35.0 // fallback

        if (rate > 0) {
            cachedRate = rate
            lastFetchTime = now
        }
        return cachedRate.takeIf { it > 0 } ?: 35.0
    }

    // API 1: exchangerate-api.com (ücretsiz, anahtar gerekmez)
    private fun tryApi1(): Double? = try {
        val json = URL("https://api.exchangerate-api.com/v4/latest/USD").readText()
        JSONObject(json).getJSONObject("rates").getDouble("TRY")
    } catch (e: Exception) { null }

    // API 2: open.er-api.com (ücretsiz, anahtar gerekmez)
    private fun tryApi2(): Double? = try {
        val json = URL("https://open.er-api.com/v6/latest/USD").readText()
        JSONObject(json).getJSONObject("rates").getDouble("TRY")
    } catch (e: Exception) { null }

    // API 3: cdn.jsdelivr.net (fawazahmed0 currency API)
    private fun tryApi3(): Double? = try {
        val json = URL("https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json").readText()
        JSONObject(json).getJSONObject("usd").getDouble("try")
    } catch (e: Exception) { null }

    fun format(usdPrice: Double, rate: Double): String {
        if (usdPrice <= 0.0) return "ÜCRETSİZ"
        val tl = usdPrice * rate
        return when {
            tl >= 1000 -> "₺${String.format("%,.0f", tl)}"
            else -> "₺${String.format("%.2f", tl)}"
        }
    }

    fun formatRaw(usdPrice: Double, rate: Double): Double = usdPrice * rate
}

// ── Game Repository ──────────────────────────────────────────────────────────
class GameRepository {
    private val api = NetworkClient.api
    private var storeCache: Map<String, Store> = emptyMap()

    private suspend fun stores(): Map<String, Store> {
        if (storeCache.isEmpty()) {
            storeCache = try { api.getStores().associateBy { it.storeId } } catch (e: Exception) { emptyMap() }
        }
        return storeCache
    }

    suspend fun search(query: String): List<GameSearchResult> = api.searchGames(query)

    suspend fun getFeaturedDeals(): List<DealDetail> = api.getDeals()

    suspend fun getGamePrices(gameId: String): Triple<GameInfo, List<PlatformPrice>, Double> {
        val rate = ExchangeRateManager.getUsdToTry()
        val info = api.getGameInfo(gameId)
        val s = stores()
        val minPrice = info.deals.mapNotNull { it.price.toDoubleOrNull() }.minOrNull() ?: 0.0

        val prices = info.deals.map { deal ->
            val store = s[deal.storeId]
            val priceUsd = deal.price.toDoubleOrNull() ?: 0.0
            val retailUsd = deal.retailPrice.toDoubleOrNull() ?: priceUsd
            PlatformPrice(
                storeId = deal.storeId,
                storeName = store?.storeName ?: platformName(deal.storeId),
                logoUrl = store?.let { NetworkClient.logoUrl(it.images.logo) } ?: "",
                currentPrice = priceUsd,
                originalPrice = retailUsd,
                savingsPercent = deal.savings.toDoubleOrNull() ?: 0.0,
                dealId = deal.dealId,
                isBestDeal = priceUsd == minPrice
            )
        }.sortedBy { it.currentPrice }

        return Triple(info, prices, rate)
    }

    suspend fun getCurrentRate(): Double = ExchangeRateManager.getUsdToTry()

    private fun platformName(id: String) = when(id) {
        "1" -> "Steam"; "25" -> "Epic Games"; "7" -> "GOG"
        "11" -> "Humble Bundle"; "15" -> "Fanatical"; "23" -> "GameBillet"
        "24" -> "Voidu"; "28" -> "IndieGala"; "8" -> "GamersGate"
        "21" -> "WinGameStore"; "13" -> "Gamesplanet"; "2" -> "GamersGate"
        else -> "Platform $id"
    }
}
