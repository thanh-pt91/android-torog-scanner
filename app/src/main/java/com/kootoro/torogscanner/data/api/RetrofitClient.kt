package com.kootoro.torogscanner.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    const val DEFAULT_SERVER_URL = "https://torog-scanner-sg.onrender.com"

    private var currentBaseUrl = DEFAULT_SERVER_URL
    private var cachedService: ToroGApiService? = null

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    fun getService(baseUrl: String = currentBaseUrl): ToroGApiService {
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        if (cachedService != null && currentBaseUrl == normalizedUrl) {
            return cachedService!!
        }

        currentBaseUrl = normalizedUrl
        val retrofit = Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(ToroGApiService::class.java)
        cachedService = service
        return service
    }

    fun updateBaseUrl(newUrl: String) {
        val trimmed = newUrl.trim()
        if (trimmed.isNotEmpty()) {
            val normalized = if (trimmed.endsWith("/")) trimmed else "$trimmed/"
            if (normalized != currentBaseUrl) {
                currentBaseUrl = normalized
                cachedService = null
            }
        }
    }
}
