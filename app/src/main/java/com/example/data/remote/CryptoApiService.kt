package com.example.data.remote

import com.example.data.model.ChartResponse
import com.example.data.model.ListingResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface CryptoApiService {

    @GET("data-api/v3/cryptocurrency/listing")
    suspend fun getCryptocurrencyListing(
        @Query("start") start: Int,
        @Query("limit") limit: Int,
        @Query("sortBy") sortBy: String = "rank",
        @Query("sortType") sortType: String = "desc",
        @Query("convert") convert: String = "USD",
        @Query("cryptoType") cryptoType: String = "all",
        @Query("tagType") tagType: String = "all",
        @Query("audited") audited: Boolean = false,
        @Query("aux") aux: String = "ath,atl,high24h,low24h,num_market_pairs,cmc_rank,date_added,max_supply,circulating_supply,total_supply,volume_7d,volume_30d,self_reported_circulating_supply,self_reported_market_cap"
    ): ListingResponse

    @GET("data-api/v3.3/cryptocurrency/detail/chart")
    suspend fun getChartData(
        @Query("id") id: Int,
        @Query("interval") interval: String,
        @Query("range") range: String,
        @Query("convertId") convertId: Int = 2781
    ): ChartResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://api.coinmarketcap.com/"

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val apiService: CryptoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(CryptoApiService::class.java)
    }
}
