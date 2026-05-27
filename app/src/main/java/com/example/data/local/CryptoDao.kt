package com.example.data.local

import androidx.room.*
import com.example.data.model.CryptoCacheEntity
import com.example.data.model.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CryptoDao {

    // Cache operations
    @Query("SELECT * FROM crypto_cache ORDER BY cmcRank ASC")
    fun getCachedCryptos(): Flow<List<CryptoCacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCryptos(cryptos: List<CryptoCacheEntity>)

    @Query("DELETE FROM crypto_cache")
    suspend fun clearCachedCryptos()

    // Favorites operations
    @Query("SELECT * FROM favorites")
    fun getFavorites(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun removeFavorite(id: Int)
}
