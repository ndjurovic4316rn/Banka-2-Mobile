package com.example.banka_2_mobile.ui.cards

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
import androidx.compose.foundation.layout.aspectRatio
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
import com.example.banka_2_mobile.data.model.CardResponse
import com.example.banka_2_mobile.data.repository.AuthRepository
import com.example.banka_2_mobile.ui.theme.DarkBg
import com.example.banka_2_mobile.ui.theme.DarkCard
import com.example.banka_2_mobile.ui.theme.ErrorRed
import com.example.banka_2_mobile.ui.theme.Indigo400
import com.example.banka_2_mobile.ui.theme.Indigo500
import com.example.banka_2_mobile.ui.theme.Indigo600
import com.example.banka_2_mobile.ui.theme.SuccessGreen
import com.example.banka_2_mobile.ui.theme.TextMuted
import com.example.banka_2_mobile.ui.theme.Violet600
import com.example.banka_2_mobile.ui.theme.Violet700
import com.example.banka_2_mobile.ui.theme.WarningYellow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen(
    onLogout: () -> Unit,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var cards by remember { mutableStateOf<List<CardResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    suspend fun fetchCards() {
        try {
            val response = RetrofitClient.api.getMyCards()
            if (response.isSuccessful) {
                cards = response.body() ?: emptyList()
            } else if (response.code() == 401) {
                authRepository.clearTokens()
                onLogout()
                return
            } else {
                errorMessage = "Greska pri ucitavanju kartica (${response.code()})"
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
        fetchCards()
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
                scope.launch { fetchCards() }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBg)
            ) {
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .alpha(orbAlpha)
                        .align(Alignment.TopStart)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Indigo500.copy(alpha = 0.3f), Color.Transparent),
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
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
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
                                    text = "Kartice",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Vase platne kartice",
                                fontSize = 13.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(start = 14.dp)
                            )
                        }

                        if (cards.isEmpty()) {
                            item { EmptyCardsState() }
                        } else {
                            items(cards) { card ->
                                CreditCardVisual(card)
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
private fun CreditCardVisual(card: CardResponse) {
    val gradient = cardGradient(card.cardType)
    val statusColor = cardStatusColor(card.status)
    val statusLabel = cardStatusLabel(card.status)
    val maskedNumber = maskCardNumber(card.cardNumber)
    val cardTypeLabel = cardTypeLabel(card.cardType)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.586f) // Standard credit card ratio
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = gradient,
                    start = Offset(0f, 0f),
                    end = Offset(800f, 400f)
                )
            )
    ) {
        // Decorative circles
        Box(
            modifier = Modifier
                .size(200.dp)
                .alpha(0.1f)
                .align(Alignment.TopEnd)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color.Transparent),
                        radius = 250f
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(150.dp)
                .alpha(0.08f)
                .align(Alignment.BottomStart)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color.Transparent),
                        radius = 200f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: card type + status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = cardTypeLabel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.2f))
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

            // Middle: chip placeholder
            Box(
                modifier = Modifier
                    .size(width = 45.dp, height = 32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.25f))
            )

            // Card number
            Text(
                text = maskedNumber,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                color = Color.White,
                letterSpacing = 3.sp
            )

            // Bottom: name + expiry
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "VLASNIK",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = (card.ownerName ?: card.cardName ?: "IME PREZIME").uppercase(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "VAZI DO",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = formatExpiry(card.expirationDate),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }
    }

    // Card limit info below the visual card
    if (card.cardLimit != null && card.cardLimit > 0) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DarkCard)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Limit kartice",
                    fontSize = 13.sp,
                    color = TextMuted
                )
                Text(
                    text = formatLimit(card.cardLimit),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun EmptyCardsState() {
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
            text = "Nema kartica",
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Nemate nijednu izdatu karticu",
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
                .width(120.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(DarkCard)
        )
        Spacer(modifier = Modifier.height(24.dp))
        repeat(2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.586f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkCard)
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ─── Helpers ───────────────────────────────────────────────

private fun maskCardNumber(number: String?): String {
    if (number == null || number.length < 4) return "**** **** **** ****"
    val last4 = number.takeLast(4)
    return "**** **** **** $last4"
}

private fun formatExpiry(date: String?): String {
    if (date == null) return "MM/YY"
    return try {
        // Try to extract MM/YY from various formats
        if (date.contains("/")) return date
        val parts = date.take(10).split("-")
        if (parts.size >= 2) "${parts[1]}/${parts[0].takeLast(2)}" else date
    } catch (e: Exception) {
        date.take(5)
    }
}

private fun formatLimit(limit: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("sr", "RS"))
    formatter.minimumFractionDigits = 2
    formatter.maximumFractionDigits = 2
    return "${formatter.format(limit)} RSD"
}

private fun cardGradient(type: String?): List<Color> {
    return when (type?.uppercase()) {
        "VISA" -> listOf(Color(0xFF1A237E), Color(0xFF283593), Color(0xFF1A237E))
        "MASTERCARD" -> listOf(Color(0xFFB71C1C), Color(0xFFE65100), Color(0xFFBF360C))
        "DEBIT", "DEBITNA" -> listOf(Indigo500, Indigo600, Violet600)
        "CREDIT", "KREDITNA" -> listOf(Violet600, Violet700, Color(0xFF581C87))
        else -> listOf(Indigo500, Violet600, Indigo600)
    }
}

private fun cardTypeLabel(type: String?): String {
    return when (type?.uppercase()) {
        "VISA" -> "VISA"
        "MASTERCARD" -> "MASTERCARD"
        "DEBIT", "DEBITNA" -> "DEBITNA KARTICA"
        "CREDIT", "KREDITNA" -> "KREDITNA KARTICA"
        else -> type?.uppercase() ?: "KARTICA"
    }
}

private fun cardStatusColor(status: String?): Color {
    return when (status?.uppercase()) {
        "ACTIVE", "AKTIVNA" -> SuccessGreen
        "BLOCKED", "BLOKIRANA" -> ErrorRed
        "EXPIRED", "ISTEKLA" -> WarningYellow
        else -> TextMuted
    }
}

private fun cardStatusLabel(status: String?): String {
    return when (status?.uppercase()) {
        "ACTIVE", "AKTIVNA" -> "Aktivna"
        "BLOCKED", "BLOKIRANA" -> "Blokirana"
        "EXPIRED", "ISTEKLA" -> "Istekla"
        else -> status ?: "Nepoznat"
    }
}
