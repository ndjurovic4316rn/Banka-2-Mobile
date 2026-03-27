package com.example.banka_2_mobile.ui.home

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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.banka_2_mobile.data.api.RetrofitClient
import com.example.banka_2_mobile.data.model.Account
import com.example.banka_2_mobile.data.repository.AuthRepository
import com.example.banka_2_mobile.ui.theme.DarkBg
import com.example.banka_2_mobile.ui.theme.DarkCard
import com.example.banka_2_mobile.ui.theme.Indigo400
import com.example.banka_2_mobile.ui.theme.Indigo500
import com.example.banka_2_mobile.ui.theme.SuccessGreen
import com.example.banka_2_mobile.ui.theme.TextMuted
import com.example.banka_2_mobile.ui.theme.Violet600
import com.example.banka_2_mobile.ui.theme.WarningYellow
import com.example.banka_2_mobile.ui.theme.ErrorRed
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var accounts by remember { mutableStateOf<List<Account>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val email = authRepository.getEmail() ?: ""
    val displayName = extractNameFromEmail(email)

    suspend fun fetchAccounts() {
        try {
            val response = RetrofitClient.api.getMyAccounts()
            if (response.isSuccessful) {
                accounts = response.body() ?: emptyList()
            } else if (response.code() == 401) {
                authRepository.clearTokens()
                onLogout()
                return
            } else {
                errorMessage = "Greska pri ucitavanju naloga (${response.code()})"
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
        fetchAccounts()
    }

    // Background animation
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val orbAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.25f,
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
                scope.launch { fetchAccounts() }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBg)
            ) {
                // Background orbs
                Box(
                    modifier = Modifier
                        .size(350.dp)
                        .alpha(orbAlpha)
                        .align(Alignment.TopEnd)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Indigo500.copy(alpha = 0.3f), Color.Transparent),
                                radius = 500f
                            )
                        )
                )

                if (isLoading) {
                    // Shimmer loading
                    LoadingShimmer()
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Zdravo,",
                                        fontSize = 14.sp,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = displayName,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Total balance card
                        item {
                            TotalBalanceCard(accounts)
                        }

                        // Section title
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
                                    text = "Vasi racuni",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }

                        if (accounts.isEmpty()) {
                            item {
                                EmptyAccountsState()
                            }
                        } else {
                            items(accounts) { account ->
                                AccountCard(account)
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

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun TotalBalanceCard(accounts: List<Account>) {
    val rsdAccounts = accounts.filter { (it.currency ?: "RSD").uppercase() == "RSD" }
    val totalRsd = rsdAccounts.sumOf { it.balance }
    val formattedBalance = formatCurrency(totalRsd, "RSD")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Indigo500, Violet600),
                    start = Offset(0f, 0f),
                    end = Offset(800f, 400f)
                )
            )
            .padding(24.dp)
    ) {
        Column {
            Text(
                text = "Ukupno stanje",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formattedBalance,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.Default
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${accounts.size} ${accountCountLabel(accounts.size)}",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun AccountCard(account: Account) {
    val currency = (account.currency ?: "RSD").uppercase()
    val gradient = currencyGradient(currency)
    val statusColor = statusColor(account.status)
    val statusLabel = statusLabel(account.status)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Currency badge
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(brush = Brush.linearGradient(gradient)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currency,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = account.name ?: account.accountType ?: "Racun",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Text(
                            text = account.accountNumber,
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }

                // Status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Balance
            Text(
                text = formatCurrency(account.balance, currency),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (account.availableBalance != account.balance) {
                Text(
                    text = "Raspolozivo: ${formatCurrency(account.availableBalance, currency)}",
                    fontSize = 12.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyAccountsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(DarkCard),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "\uD83C\uDFE6",
                fontSize = 28.sp
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Nema racuna",
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Nemate nijedan aktivan racun",
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
        // Header shimmer
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(DarkCard)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(DarkCard)
        )
        Spacer(modifier = Modifier.height(24.dp))
        // Balance card shimmer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(DarkCard)
        )
        Spacer(modifier = Modifier.height(24.dp))
        // Account card shimmers
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCard)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ─── Utility Functions ─────────────────────────────────────

private fun extractNameFromEmail(email: String): String {
    val localPart = email.substringBefore("@")
    val parts = localPart.split(".")
    return parts.joinToString(" ") { part ->
        part.replaceFirstChar { it.uppercaseChar() }
    }
}

private fun formatCurrency(amount: Double, currency: String): String {
    val formatter = NumberFormat.getNumberInstance(Locale("sr", "RS"))
    formatter.minimumFractionDigits = 2
    formatter.maximumFractionDigits = 2
    return "${formatter.format(amount)} $currency"
}

private fun accountCountLabel(count: Int): String {
    return when {
        count == 1 -> "racun"
        count in 2..4 -> "racuna"
        else -> "racuna"
    }
}

private fun currencyGradient(currency: String): List<Color> {
    return when (currency.uppercase()) {
        "RSD" -> listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))       // blue
        "EUR" -> listOf(Indigo500, Violet600)                        // indigo-violet
        "USD" -> listOf(Color(0xFF22C55E), Color(0xFF16A34A))       // green
        "CHF" -> listOf(Color(0xFFEF4444), Color(0xFFDC2626))       // red
        "GBP" -> listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED))       // purple
        "JPY" -> listOf(Color(0xFFF59E0B), Color(0xFFD97706))       // amber
        "CAD" -> listOf(Color(0xFFEC4899), Color(0xFFDB2777))       // pink
        "AUD" -> listOf(Color(0xFF14B8A6), Color(0xFF0D9488))       // teal
        else -> listOf(Color(0xFF6B7280), Color(0xFF4B5563))        // gray
    }
}

private fun statusColor(status: String?): Color {
    return when (status?.uppercase()) {
        "ACTIVE", "AKTIVNA" -> SuccessGreen
        "PENDING", "NA_CEKANJU" -> WarningYellow
        "BLOCKED", "BLOKIRAN", "FROZEN", "ZAMRZNUT" -> ErrorRed
        else -> TextMuted
    }
}

private fun statusLabel(status: String?): String {
    return when (status?.uppercase()) {
        "ACTIVE", "AKTIVNA" -> "Aktivan"
        "PENDING", "NA_CEKANJU" -> "Na cekanju"
        "BLOCKED", "BLOKIRAN" -> "Blokiran"
        "FROZEN", "ZAMRZNUT" -> "Zamrznut"
        else -> status ?: "Nepoznat"
    }
}
