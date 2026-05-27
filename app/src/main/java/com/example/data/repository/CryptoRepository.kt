package com.example.data.repository

import com.example.data.local.CryptoDao
import com.example.data.model.*
import com.example.data.remote.CryptoApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class CryptoRepository(
    private val apiService: CryptoApiService,
    private val cryptoDao: CryptoDao
) {

    // Observe active favorites mapped to a set of IDs
    val favoriteIds: Flow<Set<Int>> = cryptoDao.getFavorites()
        .map { list -> list.map { it.id }.toSet() }

    // Observe database cache combined with favorites in real time
    val cachedCryptos: Flow<List<CryptocurrencyUiModel>> = combine(
        cryptoDao.getCachedCryptos(),
        favoriteIds
    ) { cachedList, favs ->
        cachedList.map { entity ->
            entity.toDomain(isFavorite = favs.contains(entity.id))
        }
    }

    suspend fun loadCachedFirstPage(): List<CryptocurrencyUiModel> {
        val cached = cryptoDao.getCachedCryptos().firstOrNull() ?: emptyList()
        val favs = cryptoDao.getFavorites().firstOrNull()?.map { it.id }?.toSet() ?: emptySet()
        return cached.map { it.toDomain(isFavorite = favs.contains(it.id)) }
    }

    suspend fun refreshFirstPage(limit: Int): List<CryptocurrencyUiModel> {
        val response = apiService.getCryptocurrencyListing(start = 1, limit = limit)
        val apiList = response.data?.cryptoCurrencyList ?: emptyList()
        val cacheEntities = apiList.map { it.toCacheEntity() }

        // Clear previous cache & insert new first page items
        cryptoDao.clearCachedCryptos()
        cryptoDao.insertCryptos(cacheEntities)

        val currentFavs = cryptoDao.getFavorites().firstOrNull()?.map { it.id }?.toSet() ?: emptySet()
        return cacheEntities.map { it.toDomain(isFavorite = currentFavs.contains(it.id)) }
    }

    suspend fun fetchPage(start: Int, limit: Int): List<CryptocurrencyUiModel> {
        val response = apiService.getCryptocurrencyListing(start = start, limit = limit)
        val apiList = response.data?.cryptoCurrencyList ?: emptyList()
        val cacheEntities = apiList.map { it.toCacheEntity() }

        // Update database cache for newly fetched paging records
        cryptoDao.insertCryptos(cacheEntities)

        val currentFavs = cryptoDao.getFavorites().firstOrNull()?.map { it.id }?.toSet() ?: emptySet()
        return cacheEntities.map { it.toDomain(isFavorite = currentFavs.contains(it.id)) }
    }

    suspend fun getChartPoints(id: Int, interval: String, range: String): List<PricePoint> {
        val response = apiService.getChartData(id = id, interval = interval, range = range)
        val points = response.data?.points ?: emptyList()
        return points.mapNotNull { point ->
            val timestampSec = point.s?.toLongOrNull() ?: return@mapNotNull null
            val values = point.v ?: return@mapNotNull null
            if (values.size >= 3) {
                PricePoint(
                    timestamp = timestampSec * 1000L,
                    price = values[0],
                    volume = values[1],
                    marketCap = values[2]
                )
            } else null
        }
    }

    suspend fun toggleFavorite(id: Int, shouldFavorite: Boolean) {
        if (shouldFavorite) {
            cryptoDao.addFavorite(FavoriteEntity(id))
        } else {
            cryptoDao.removeFavorite(id)
        }
    }

    // Mapper helper extensions inside Repository context
    private fun ApiCryptoCurrency.toCacheEntity(): CryptoCacheEntity {
        val usdQuote = quotes?.firstOrNull { it.name == "USD" }
        return CryptoCacheEntity(
            id = id,
            name = name ?: "Unknown",
            symbol = symbol ?: "",
            slug = slug ?: "",
            cmcRank = cmcRank ?: 9999,
            circulatingSupply = circulatingSupply ?: 0.0,
            totalSupply = totalSupply ?: 0.0,
            maxSupply = maxSupply ?: 0.0,
            price = usdQuote?.price ?: 0.0,
            volume24h = usdQuote?.volume24h ?: 0.0,
            volumePercentChange = usdQuote?.volumePercentChange ?: 0.0,
            marketCap = usdQuote?.marketCap ?: 0.0,
            percentChange1h = usdQuote?.percentChange1h ?: 0.0,
            percentChange24h = usdQuote?.percentChange24h ?: 0.0,
            percentChange7d = usdQuote?.percentChange7d ?: 0.0,
            percentChange30d = usdQuote?.percentChange30d ?: 0.0,
            lastUpdated = lastUpdated ?: ""
        )
    }

    private fun CryptoCacheEntity.toDomain(isFavorite: Boolean): CryptocurrencyUiModel {
        return CryptocurrencyUiModel(
            id = id,
            name = name,
            symbol = symbol,
            slug = slug,
            cmcRank = cmcRank,
            circulatingSupply = circulatingSupply,
            totalSupply = totalSupply,
            maxSupply = maxSupply,
            price = price,
            volume24h = volume24h,
            volumePercentChange = volumePercentChange,
            marketCap = marketCap,
            percentChange1h = percentChange1h,
            percentChange24h = percentChange24h,
            percentChange7d = percentChange7d,
            percentChange30d = percentChange30d,
            lastUpdated = lastUpdated,
            isFavorite = isFavorite
        )
    }
}
