package com.dealhunt.app.ui

import androidx.lifecycle.*
import com.dealhunt.app.data.ExchangeRateManager
import com.dealhunt.app.data.GameRepository
import com.dealhunt.app.model.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val msg: String) : UiState<Nothing>()
}

class MainViewModel : ViewModel() {
    private val repo = GameRepository()

    private val _search = MutableLiveData<UiState<List<GameSearchResult>>>(UiState.Idle)
    val search: LiveData<UiState<List<GameSearchResult>>> = _search

    private val _deals = MutableLiveData<UiState<List<DealDetail>>>(UiState.Loading)
    val deals: LiveData<UiState<List<DealDetail>>> = _deals

    private val _rate = MutableLiveData<Double>(35.0)
    val rate: LiveData<Double> = _rate

    private var searchJob: Job? = null

    init { loadDeals() }

    fun searchGames(query: String) {
        if (query.isBlank()) { _search.value = UiState.Idle; return }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            _search.value = UiState.Loading
            try {
                val r = repo.search(query)
                _search.value = if (r.isEmpty()) UiState.Error("'$query' için sonuç bulunamadı") else UiState.Success(r)
            } catch (e: Exception) {
                _search.value = UiState.Error("Bağlantı hatası. İnternet bağlantını kontrol et.")
            }
        }
    }

    fun clearSearch() { searchJob?.cancel(); _search.value = UiState.Idle }

    fun loadDeals() {
        viewModelScope.launch {
            _deals.value = UiState.Loading
            try {
                val rateDeferred = async { repo.getCurrentRate() }
                val dealsDeferred = async { repo.getFeaturedDeals() }
                _rate.value = rateDeferred.await()
                val dealList = dealsDeferred.await()
                _deals.value = if (dealList.isEmpty()) UiState.Error("Fırsat bulunamadı") else UiState.Success(dealList)
            } catch (e: Exception) {
                _deals.value = UiState.Error("Yüklenemedi: ${e.message}")
            }
        }
    }

    fun searchByGenre(genre: String) { searchGames(genre) }

    fun getStoreName(storeId: String) = repo.getStoreName(storeId)
}

class DetailViewModel : ViewModel() {
    private val repo = GameRepository()

    private val _detail = MutableLiveData<UiState<GameDetailUiState>>(UiState.Loading)
    val detail: LiveData<UiState<GameDetailUiState>> = _detail

    private val _rate = MutableLiveData<Double>(35.0)
    val rate: LiveData<Double> = _rate

    fun load(gameId: String) {
        viewModelScope.launch {
            _detail.value = UiState.Loading
            try {
                val (info, prices, rate) = repo.getGamePrices(gameId)
                _rate.value = rate
                val fmt = SimpleDateFormat("dd MMM yyyy", Locale("tr"))
                val cheapDate = if (info.cheapestPriceEver.date > 0)
                    fmt.format(Date(info.cheapestPriceEver.date * 1000)) else "Bilinmiyor"
                val cheapUsd = info.cheapestPriceEver.price.toDoubleOrNull() ?: 0.0
                _detail.value = UiState.Success(GameDetailUiState(
                    title = info.info.title,
                    thumbnail = info.info.thumbnail,
                    steamAppId = info.info.steamAppId,
                    platformPrices = prices,
                    cheapestEver = ExchangeRateManager.format(cheapUsd, rate),
                    cheapestEverDate = cheapDate
                ))
            } catch (e: Exception) {
                _detail.value = UiState.Error("Detaylar yüklenemedi: ${e.message}")
            }
        }
    }
}
