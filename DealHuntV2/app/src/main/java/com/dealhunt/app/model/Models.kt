package com.dealhunt.app.model
import com.google.gson.annotations.SerializedName

data class GameSearchResult(
    @SerializedName("gameID") val gameId: String = "",
    @SerializedName("steamAppID") val steamAppId: String? = null,
    @SerializedName("cheapest") val cheapest: String = "0",
    @SerializedName("cheapestDealID") val cheapestDealId: String = "",
    @SerializedName("external") val title: String = "",
    @SerializedName("thumb") val thumbnail: String = "",
    @SerializedName("internalName") val internalName: String = ""
)

data class DealDetail(
    @SerializedName("dealID") val dealId: String = "",
    @SerializedName("storeID") val storeId: String = "",
    @SerializedName("gameID") val gameId: String = "",
    @SerializedName("salePrice") val salePrice: String = "0",
    @SerializedName("normalPrice") val normalPrice: String = "0",
    @SerializedName("savings") val savings: String = "0",
    @SerializedName("metacriticScore") val metacriticScore: String = "0",
    @SerializedName("title") val title: String = "",
    @SerializedName("thumb") val thumbnail: String = ""
)

data class Store(
    @SerializedName("storeID") val storeId: String,
    @SerializedName("storeName") val storeName: String,
    @SerializedName("isActive") val isActive: Int,
    @SerializedName("images") val images: StoreImages
)

data class StoreImages(
    @SerializedName("logo") val logo: String = "",
    @SerializedName("icon") val icon: String = ""
)

data class GameInfo(
    @SerializedName("info") val info: GameInfoDetail,
    @SerializedName("cheapestPriceEver") val cheapestPriceEver: CheapestPriceEver,
    @SerializedName("deals") val deals: List<GameDealItem>
)

data class GameInfoDetail(
    @SerializedName("title") val title: String = "",
    @SerializedName("steamAppID") val steamAppId: String? = null,
    @SerializedName("thumb") val thumbnail: String = ""
)

data class CheapestPriceEver(
    @SerializedName("price") val price: String = "0",
    @SerializedName("date") val date: Long = 0
)

data class GameDealItem(
    @SerializedName("storeID") val storeId: String = "",
    @SerializedName("dealID") val dealId: String = "",
    @SerializedName("price") val price: String = "0",
    @SerializedName("retailPrice") val retailPrice: String = "0",
    @SerializedName("savings") val savings: String = "0"
)

data class PlatformPrice(
    val storeId: String, val storeName: String, val logoUrl: String,
    val currentPrice: Double, val originalPrice: Double,
    val savingsPercent: Double, val dealId: String, val isBestDeal: Boolean = false
)

data class GameDetailUiState(
    val title: String, val thumbnail: String, val steamAppId: String?,
    val platformPrices: List<PlatformPrice>, val cheapestEver: String, val cheapestEverDate: String
)
