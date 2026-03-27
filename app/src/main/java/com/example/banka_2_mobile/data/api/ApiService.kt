package com.example.banka_2_mobile.data.api

import com.example.banka_2_mobile.data.model.Account
import com.example.banka_2_mobile.data.model.CalculateExchangeResponse
import com.example.banka_2_mobile.data.model.CardResponse
import com.example.banka_2_mobile.data.model.ExchangeRate
import com.example.banka_2_mobile.data.model.LoginRequest
import com.example.banka_2_mobile.data.model.LoginResponse
import com.example.banka_2_mobile.data.model.OtpResponse
import com.example.banka_2_mobile.data.model.PaginatedResponse
import com.example.banka_2_mobile.data.model.Payment
import com.example.banka_2_mobile.data.model.RefreshRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    // ─── Auth ──────────────────────────────────────────

    @POST("auth/login")
    suspend fun login(@Body credentials: LoginRequest): Response<LoginResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshRequest): Response<LoginResponse>

    // ─── OTP ───────────────────────────────────────────

    @GET("payments/my-otp")
    suspend fun getActiveOtp(): Response<OtpResponse>

    // ─── Accounts ──────────────────────────────────────

    @GET("accounts/my")
    suspend fun getMyAccounts(): Response<List<Account>>

    // ─── Payments / Transactions ───────────────────────

    @GET("payments")
    suspend fun getPayments(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PaginatedResponse<Payment>>

    // ─── Cards ─────────────────────────────────────────

    @GET("cards")
    suspend fun getMyCards(): Response<List<CardResponse>>

    // ─── Exchange ──────────────────────────────────────

    @GET("exchange-rates")
    suspend fun getExchangeRates(): Response<List<ExchangeRate>>

    @GET("exchange/calculate")
    suspend fun calculateExchange(
        @Query("amount") amount: Double,
        @Query("fromCurrency") fromCurrency: String,
        @Query("toCurrency") toCurrency: String
    ): Response<CalculateExchangeResponse>
}
