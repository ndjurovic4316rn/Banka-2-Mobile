package com.example.banka_2_mobile.data.model

import com.google.gson.annotations.SerializedName

// ─── Auth Models ───────────────────────────────────────────

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    @SerializedName("accessToken")
    val accessToken: String,
    @SerializedName("refreshToken")
    val refreshToken: String
)

data class RefreshRequest(
    val refreshToken: String
)

data class OtpResponse(
    val active: Boolean,
    val code: String? = null,
    val expiresInSeconds: Int? = null,
    val attempts: Int? = null,
    val maxAttempts: Int? = null,
    val message: String? = null
)

// ─── Account Models ────────────────────────────────────────

data class Account(
    val id: Long,
    val accountNumber: String,
    val name: String? = null,
    val accountType: String? = null,
    val subType: String? = null,
    val currency: String? = null,
    val balance: Double = 0.0,
    val availableBalance: Double = 0.0,
    val status: String? = null,
    val dailyLimit: Double? = null,
    val monthlyLimit: Double? = null,
    val createdAt: String? = null,
    val ownerName: String? = null,
    val companyName: String? = null
)

// ─── Payment / Transaction Models ──────────────────────────

data class Payment(
    val id: Long,
    val orderNumber: String? = null,
    val fromAccount: String? = null,
    val toAccount: String? = null,
    val amount: Double = 0.0,
    val currency: String? = null,
    val status: String? = null,
    val direction: String? = null,
    val description: String? = null,
    val recipientName: String? = null,
    val createdAt: String? = null,
    val fee: Double? = null,
    val paymentCode: String? = null,
    val referenceNumber: String? = null
)

data class PaginatedResponse<T>(
    val content: List<T> = emptyList(),
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val number: Int = 0,
    val size: Int = 0
)

// ─── Card Models ───────────────────────────────────────────

data class CardResponse(
    val id: Long,
    val cardNumber: String? = null,
    val cardType: String? = null,
    val status: String? = null,
    val cardLimit: Double? = null,
    val accountId: Long? = null,
    val accountNumber: String? = null,
    val cardName: String? = null,
    val expirationDate: String? = null,
    val ownerName: String? = null,
    val cvv: String? = null
)

// ─── Exchange Models ───────────────────────────────────────

data class ExchangeRate(
    val currency: String,
    val rate: Double? = null,
    val buyRate: Double? = null,
    val sellRate: Double? = null,
    val middleRate: Double? = null,
    val date: String? = null
)

data class CalculateExchangeResponse(
    val convertedAmount: Double,
    val exchangeRate: Double,
    val fromCurrency: String,
    val toCurrency: String
)
