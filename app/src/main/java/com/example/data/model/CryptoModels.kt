package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

// Database Entities
@Entity(tableName = "crypto_cache")
data class CryptoCacheEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val symbol: String,
    val slug: String,
    val cmcRank: Int,
    val circulatingSupply: Double,
    val totalSupply: Double,
    val maxSupply: Double,
    val price: Double,
    val volume24h: Double,
    val volumePercentChange: Double,
    val marketCap: Double,
    val percentChange1h: Double,
    val percentChange24h: Double,
    val percentChange7d: Double,
    val percentChange30d: Double,
    val lastUpdated: String
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: Int
)

// Main Domain Model (UI representation)
data class CryptocurrencyUiModel(
    val id: Int,
    val name: String,
    val symbol: String,
    val slug: String,
    val cmcRank: Int,
    val circulatingSupply: Double,
    val totalSupply: Double,
    val maxSupply: Double,
    val price: Double,
    val volume24h: Double,
    val volumePercentChange: Double,
    val marketCap: Double,
    val percentChange1h: Double,
    val percentChange24h: Double,
    val percentChange7d: Double,
    val percentChange30d: Double,
    val lastUpdated: String,
    val isFavorite: Boolean = false
) {
    val imageUrl: String
        get() = "https://s2.coinmarketcap.com/static/img/coins/64x64/$id.png"
}

// API Network Models
@JsonClass(generateAdapter = true)
data class ListingResponse(
    val data: ListingData?,
    val status: ListingStatus?
)

@JsonClass(generateAdapter = true)
data class ListingData(
    val cryptoCurrencyList: List<ApiCryptoCurrency>?,
    val totalCount: String?
)

@JsonClass(generateAdapter = true)
data class ListingStatus(
    val timestamp: String?,
    val error_code: String?,
    val error_message: String?
)

@JsonClass(generateAdapter = true)
data class ApiCryptoCurrency(
    val id: Int,
    val name: String?,
    val symbol: String?,
    val slug: String?,
    val cmcRank: Int?,
    val circulatingSupply: Double?,
    val totalSupply: Double?,
    val maxSupply: Double?,
    val isActive: Int?,
    val lastUpdated: String?,
    val dateAdded: String?,
    val quotes: List<ApiQuote>?
)

@JsonClass(generateAdapter = true)
data class ApiQuote(
    val name: String?,
    val price: Double?,
    val volume24h: Double?,
    val volumePercentChange: Double?,
    val marketCap: Double?,
    val percentChange1h: Double?,
    val percentChange24h: Double?,
    val percentChange7d: Double?,
    val percentChange30d: Double?,
    val lastUpdated: String?
)

// Chart API Models
@JsonClass(generateAdapter = true)
data class ChartResponse(
    val data: ChartData?
)

@JsonClass(generateAdapter = true)
data class ChartData(
    val points: List<ChartPoint>?
)

@JsonClass(generateAdapter = true)
data class ChartPoint(
    val s: String?,
    val v: List<Double>?
)

// UI Chart Point
data class PricePoint(
    val timestamp: Long, // unix time in millis
    val price: Double,
    val volume: Double,
    val marketCap: Double
)
