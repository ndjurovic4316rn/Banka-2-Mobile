package com.example.banka_2_mobile.ui.transactions

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.banka_2_mobile.data.api.RetrofitClient
import com.example.banka_2_mobile.data.model.Payment
import com.example.banka_2_mobile.data.repository.AuthRepository
import com.example.banka_2_mobile.ui.theme.DarkBg
import com.example.banka_2_mobile.ui.theme.DarkCard
import com.example.banka_2_mobile.ui.theme.ErrorRed
import com.example.banka_2_mobile.ui.theme.Indigo500
import com.example.banka_2_mobile.ui.theme.SuccessGreen
import com.example.banka_2_mobile.ui.theme.TextMuted
import com.example.banka_2_mobile.ui.theme.Violet600
import com.example.banka_2_mobile.ui.theme.WarningYellow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onLogout: () -> Unit,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var payments by remember { mutableStateOf<List<Payment>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    suspend fun fetchPayments() {
        try {
            val response = RetrofitClient.api.getPayments(page = 0, size = 50)
            if (response.isSuccessful) {
                payments = response.body()?.content ?: emptyList()
            } else if (response.code() == 401) {
                authRepository.clearTokens()
                onLogout()
                return
            } else {
                errorMessage = "Greska pri ucitavanju transakcija (${response.code()})"
            }
        } catch (e: Exception) {
            errorMessage = "Greska u mrezi. Proverite konekciju."
        }
        isLoading = false
        isRefreshing = false
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    LaunchedEffect(Unit) {
        fetchPayments()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val orbAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                scope.launch { fetchPayments() }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBg)
            ) {
                // Background orb
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .alpha(orbAlpha)
                        .align(Alignment.BottomStart)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Violet600.copy(alpha = 0.3f), Color.Transparent),
                                radius = 400f
                            )
                        )
                )

                if (isLoading) {
                    LoadingShimmer()
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(DarkCard)
                                        .clickable { onBack() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Nazad",
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(20.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(Indigo500, Violet600)
                                            )
                                        )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Transakcije",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Istorija vasih placanja",
                                fontSize = 13.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(start = 14.dp)
                            )
                        }

                        if (payments.isEmpty()) {
                            item { EmptyTransactionsState() }
                        } else {
                            items(payments) { payment ->
                                PaymentCard(payment)
                            }
                        }

                        // Bottom spacing for navbar
                        item {
                            Spacer(modifier = Modifier.height(90.dp))
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun PaymentCard(payment: Payment) {
    val isIncoming = payment.direction?.uppercase() == "IN" || payment.direction?.uppercase() == "INCOMING"
    val amountColor = if (isIncoming) SuccessGreen else ErrorRed
    val amountPrefix = if (isIncoming) "+" else "-"
    val arrowSymbol = if (isIncoming) "\u2193" else "\u2191"  // down/up arrows
    val arrowBg = if (isIncoming) SuccessGreen else ErrorRed
    val currency = payment.currency ?: "RSD"
    val statusColor = paymentStatusColor(payment.status)
    val statusLabel = paymentStatusLabel(payment.status)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCard)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Direction arrow
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(arrowBg.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = arrowSymbol,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = arrowBg
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = payment.recipientName ?: payment.toAccount ?: "Nepoznat primalac",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (payment.description != null) {
                    Text(
                        text = payment.description,
                        fontSize = 12.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    // Date
                    Text(
                        text = formatDate(payment.createdAt),
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Status badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = statusColor
                        )
                    }
                }
            }

            // Amount
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix${formatAmount(payment.amount)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = amountColor
                )
                Text(
                    text = currency,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun EmptyTransactionsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(DarkCard),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "\uD83D\uDCB3", fontSize = 28.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Nema transakcija",
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Nemate nijednu izvrsenu transakciju",
            fontSize = 13.sp,
            color = TextMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LoadingShimmer() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(DarkCard)
        )
        Spacer(modifier = Modifier.height(24.dp))
        repeat(6) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkCard)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// ─── Helpers ───────────────────────────────────────────────

private fun formatAmount(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("sr", "RS"))
    formatter.minimumFractionDigits = 2
    formatter.maximumFractionDigits = 2
    return formatter.format(amount)
}

private fun formatDate(dateStr: String?): String {
    if (dateStr == null) return ""
    // Simple date extraction: take first 10 chars (YYYY-MM-DD) and reformat
    return try {
        val datePart = dateStr.take(10)
        val parts = datePart.split("-")
        if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}." else datePart
    } catch (e: Exception) {
        dateStr.take(10)
    }
}

private fun paymentStatusColor(status: String?): Color {
    return when (status?.uppercase()) {
        "COMPLETED", "ZAVRSENA", "SUCCESS" -> SuccessGreen
        "PENDING", "NA_CEKANJU", "PROCESSING" -> WarningYellow
        "REJECTED", "ODBIJENA", "FAILED" -> ErrorRed
        else -> TextMuted
    }
}

private fun paymentStatusLabel(status: String?): String {
    return when (status?.uppercase()) {
        "COMPLETED", "ZAVRSENA", "SUCCESS" -> "Zavrsena"
        "PENDING", "NA_CEKANJU" -> "Na cekanju"
        "PROCESSING" -> "U obradi"
        "REJECTED", "ODBIJENA" -> "Odbijena"
        "FAILED" -> "Neuspesna"
        else -> status ?: "Nepoznat"
    }
}
