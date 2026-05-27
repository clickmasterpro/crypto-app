package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.CryptocurrencyUiModel
import com.example.data.model.PricePoint
import com.example.data.repository.CryptoRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface ChartUiState {
    object Loading : ChartUiState
    data class Success(val points: List<PricePoint>) : ChartUiState
    data class Error(val message: String) : ChartUiState
}

class CryptoViewModel(private val repository: CryptoRepository) : ViewModel() {

    // Day & Night mode theme toggle state
    private val _isDarkMode = MutableStateFlow(true) // defaults to elegant dark theme
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Search query state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Sorting order options
    sealed interface SortOption {
        object RankAsc : SortOption
        object PriceDesc : SortOption
        object PriceAsc : SortOption
        object Change24hDesc : SortOption
        object Change24hAsc : SortOption
    }
    private val _sortOption = MutableStateFlow<SortOption>(SortOption.RankAsc)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    // ViewModel internal track list of fetched cryptocurrencies
    private val _apiCryptoList = MutableStateFlow<List<CryptocurrencyUiModel>>(emptyList())

    // Loading & Refreshing indicators
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Pagination metrics
    private var currentPage = 1
    private val limitPerPage = 50
    private var isLastPageLoaded = false

    // Combined list: filters and joins both paging + caching database updates!
    val cryptocurrencies: StateFlow<List<CryptocurrencyUiModel>> = combine(
        _apiCryptoList,
        repository.cachedCryptos,
        _searchQuery,
        _sortOption
    ) { apiList, cachedList, query, sort ->
        // Fallback to database list if remote API list is empty (offline capability)
        val sourceList = if (apiList.isEmpty()) cachedList else {
            // Re-sync favorites dynamically for apiList from repo's reactive sets
            val favIds = repository.favoriteIds.firstOrNull() ?: emptySet()
            apiList.map { it.copy(isFavorite = favIds.contains(it.id)) }
        }

        // Apply search filter
        val filtered = if (query.isBlank()) {
            sourceList
        } else {
            sourceList.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.symbol.contains(query, ignoreCase = true)
            }
        }

        // Apply sort rule
        when (sort) {
            SortOption.RankAsc -> filtered.sortedBy { it.cmcRank }
            SortOption.PriceDesc -> filtered.sortedByDescending { it.price }
            SortOption.PriceAsc -> filtered.sortedBy { it.price }
            SortOption.Change24hDesc -> filtered.sortedByDescending { it.percentChange24h }
            SortOption.Change24hAsc -> filtered.sortedBy { it.percentChange24h }
        }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // List of favorites only, derived from current cryptocurrencies list
    val favorites: StateFlow<List<CryptocurrencyUiModel>> = repository.cachedCryptos
        .map { list -> list.filter { it.isFavorite } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected cryptocurrency details state
    private val _selectedCoin = MutableStateFlow<CryptocurrencyUiModel?>(null)
    val selectedCoin: StateFlow<CryptocurrencyUiModel?> = _selectedCoin.asStateFlow()

    // Charting state
    private val _chartUiState = MutableStateFlow<ChartUiState>(ChartUiState.Loading)
    val chartUiState: StateFlow<ChartUiState> = _chartUiState.asStateFlow()

    val chartTimeframe = MutableStateFlow("1D") // options: 1D, 7D, 30D, 1Y

    init {
        // Pre-load from DB cache instantly
        viewModelScope.launch {
            val cached = repository.loadCachedFirstPage()
            if (cached.isNotEmpty()) {
                _apiCryptoList.value = cached
            }
            // Trigger remote update immediately
            refreshData()
        }

        // Watch timeframe changes to reload details chart
        viewModelScope.launch {
            chartTimeframe.collect { timeframe ->
                _selectedCoin.value?.let { loadChartFor(it, timeframe) }
            }
        }
    }

    fun toggleTheme() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun selectCoin(coin: CryptocurrencyUiModel) {
        _selectedCoin.value = coin
        // Trigger default chart timeframe load
        chartTimeframe.value = "1D"
        loadChartFor(coin, "1D")
    }

    fun toggleFavorite(coin: CryptocurrencyUiModel) {
        viewModelScope.launch {
            val targetFav = !coin.isFavorite
            repository.toggleFavorite(coin.id, targetFav)

            // Update local state lists reactively
            _apiCryptoList.value = _apiCryptoList.value.map {
                if (it.id == coin.id) it.copy(isFavorite = targetFav) else it
            }
            if (_selectedCoin.value?.id == coin.id) {
                _selectedCoin.value = _selectedCoin.value?.copy(isFavorite = targetFav)
            }
        }
    }

    fun refreshData() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null
            currentPage = 1
            isLastPageLoaded = false
            try {
                val firstPage = repository.refreshFirstPage(limitPerPage)
                _apiCryptoList.value = firstPage
            } catch (e: Exception) {
                _errorMessage.value = "Failed to synchronize: ${e.localizedMessage ?: "Offline cache used"}"
                // Keep local data from cache
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun loadNextPage() {
        if (_isLoading.value || isLastPageLoaded) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                currentPage += 1
                val startIndex = (currentPage - 1) * limitPerPage + 1
                val nextPage = repository.fetchPage(start = startIndex, limit = limitPerPage)
                if (nextPage.isEmpty()) {
                    isLastPageLoaded = true
                } else {
                    val currentList = _apiCryptoList.value.toMutableList()
                    // Filter duplicates
                    val existingIds = currentList.map { it.id }.toSet()
                    val filteredNextPage = nextPage.filter { !existingIds.contains(it.id) }
                    currentList.addAll(filteredNextPage)
                    _apiCryptoList.value = currentList
                }
            } catch (e: Exception) {
                _errorMessage.value = "Paging error: ${e.localizedMessage ?: "Network error"}"
                currentPage -= 1 // revert page index
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadChartFor(coin: CryptocurrencyUiModel, timeframe: String) {
        viewModelScope.launch {
            _chartUiState.value = ChartUiState.Loading
            val (range, interval) = when (timeframe) {
                "1D" -> "1D" to "5m"
                "7D" -> "7D" to "1h"
                "30D" -> "30D" to "4h"
                "1Y" -> "1Y" to "1d"
                else -> "1D" to "5m"
            }
            try {
                val points = repository.getChartPoints(coin.id, interval, range)
                if (points.isEmpty()) {
                    _chartUiState.value = ChartUiState.Error("No chart records available for $timeframe.")
                } else {
                    _chartUiState.value = ChartUiState.Success(points)
                }
            } catch (e: Exception) {
                _chartUiState.value = ChartUiState.Error("Error: ${e.localizedMessage ?: "Network trace error"}")
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
class CryptoViewModelFactory(private val repository: CryptoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CryptoViewModel::class.java)) {
            return CryptoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
