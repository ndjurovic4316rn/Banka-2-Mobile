package com.example.banka_2_mobile.data.api

import com.example.banka_2_mobile.BuildConfig
import com.example.banka_2_mobile.data.model.RefreshRequest
import com.example.banka_2_mobile.data.repository.AuthRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private var authRepository: AuthRepository? = null

    fun init(repo: AuthRepository) {
        authRepository = repo
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val url = original.url.encodedPath

        // Skip auth header for login and refresh endpoints
        if (url.contains("auth/login") || url.contains("auth/refresh")) {
            return@Interceptor chain.proceed(original)
        }

        val token = authRepository?.getAccessToken()
        val request = if (token != null) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }

        val response = chain.proceed(request)

        // Handle 401 - try to refresh the token
        if (response.code == 401 && token != null) {
            response.close()

            val refreshToken = authRepository?.getRefreshToken()
            if (refreshToken != null) {
                val newTokens = runBlocking {
                    try {
                        val refreshResponse = refreshApi.refreshToken(
                            RefreshRequest(refreshToken)
                        )
                        if (refreshResponse.isSuccessful) {
                            refreshResponse.body()
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }
                }

                if (newTokens != null) {
                    authRepository?.saveTokens(newTokens.accessToken, newTokens.refreshToken)

                    // Retry the original request with new token
                    val retryRequest = original.newBuilder()
                        .header("Authorization", "Bearer ${newTokens.accessToken}")
                        .build()
                    return@Interceptor chain.proceed(retryRequest)
                } else {
                    // Refresh failed - clear tokens
                    authRepository?.clearTokens()
                }
            }
        }

        response
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // Separate retrofit instance for refresh (no auth interceptor to avoid recursion)
    private val refreshClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val refreshRetrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(refreshClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val refreshApi: ApiService = refreshRetrofit.create(ApiService::class.java)

    val api: ApiService = retrofit.create(ApiService::class.java)
}
