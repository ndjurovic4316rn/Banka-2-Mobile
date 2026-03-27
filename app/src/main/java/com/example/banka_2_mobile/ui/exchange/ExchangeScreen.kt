package com.example.banka_2_mobile.ui.exchange

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.banka_2_mobile.data.api.RetrofitClient
import com.example.banka_2_mobile.data.model.ExchangeRate
import com.example.banka_2_mobile.data.repository.AuthRepository
import com.example.banka_2_mobile.ui.theme.DarkBg
import com.example.banka_2_mobile.ui.theme.DarkCard
import com.example.banka_2_mobile.ui.theme.DarkCardBorder
import com.example.banka_2_mobile.ui.theme.Indigo400
import com.example.banka_2_mobile.ui.theme.Indigo500
import com.example.banka_2_mobile.ui.theme.TextMuted
import com.example.banka_2_mobile.ui.theme.Violet600
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExchangeScreen(
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var rates by remember { mutableStateOf<List<ExchangeRate>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Calculator state
    var amount by remember { mutableStateOf("") }
    var fromCurrency by remember { mutableStateOf("EUR") }
    var toCurrency by remember { mutableStateOf("RSD") }
    var convertedResult by remember { mutableStateOf<String?>(null) }
    var isCalculating by remember { mutableStateOf(false) }

    suspend fun fetchRates() {
        try {
            val response = RetrofitClient.api.getExchangeRates()
            if (response.isSuccessful) {
                rates = response.body() ?: emptyList()
            } else if (response.code() == 401) {
                authRepository.clearTokens()
                onLogout()
                return
            } else {
                errorMessage = "Greska pri ucitavanju kursne liste (${response.code()})"
            }
        } catch (e: Exception) {
            errorMessage = "Greska u mrezi. Proverite konekciju."
        }
        isLoading = false
        isRefreshing = false
    }

    suspend fun calculate() {
        val amountVal = amount.replace(",", ".").toDoubleOrNull()
        if (amountVal == null || amountVal <= 0) {
            errorMessage = "Unesite ispravan iznos"
            return
        }
        isCalculating = true
        try {
            val response = RetrofitClient.api.calculateExchange(
                amount = amountVal,
                fromCurrency = fromCurrency,
                toCurrency = toCurrency
            )
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val formatter = NumberFormat.getNumberInstance(Locale("sr", "RS"))
                    formatter.minimumFractionDigits = 2
                    formatter.maximumFractionDigits = 2
                    convertedResult = "${formatter.format(body.convertedAmount)} $toCurrency"
                }
            } else {
                errorMessage = "Greska pri konverziji (${response.code()})"
            }
        } catch (e: Exception) {
            errorMessage = "Greska u mrezi"
        }
        isCalculating = false
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    LaunchedEffect(Unit) {
        fetchRates()
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
                scope.launch { fetchRates() }
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
                        .align(Alignment.TopEnd)
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
                                    text = "Kursna lista",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Kursevi valuta prema RSD",
                                fontSize = 13.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(start = 14.dp)
                            )
                        }

                        // Rate table header
                        if (rates.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Valuta",
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1.2f)
                                    )
                                    Text(
                                        text = "Kupovni",
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = "Srednji",
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = "Prodajni",
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }

                        if (rates.isEmpty()) {
                            item { EmptyRatesState() }
                        } else {
                            items(rates) { rate ->
                                RateRow(rate)
                            }
                        }

                        // Calculator section
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            CalculatorSection(
                                amount = amount,
                                onAmountChange = { amount = it },
                                fromCurrency = fromCurrency,
                                toCurrency = toCurrency,
                                onFromChange = { fromCurrency = it },
                                onToChange = { toCurrency = it },
                                result = convertedResult,
                                isCalculating = isCalculating,
                                currencies = listOf("RSD") + rates.map { it.currency },
                                onCalculate = {
                                    scope.launch { calculate() }
                                }
                            )
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
private fun RateRow(rate: ExchangeRate) {
    val gradient = currencyBadgeGradient(rate.currency)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Currency badge + name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1.2f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(brush = Brush.linearGradient(gradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = rate.currency.take(3),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = rate.currency,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }

            Text(
                text = formatRate(rate.buyRate),
                fontSize = 13.sp,
                color = Color.White,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
            Text(
                text = formatRate(rate.middleRate ?: rate.rate),
                fontSize = 13.sp,
                color = Indigo400,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
            Text(
                text = formatRate(rate.sellRate),
                fontSize = 13.sp,
                color = Color.White,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun CalculatorSection(
    amount: String,
    onAmountChange: (String) -> Unit,
    fromCurrency: String,
    toCurrency: String,
    onFromChange: (String) -> Unit,
    onToChange: (String) -> Unit,
    result: String?,
    isCalculating: Boolean,
    currencies: List<String>,
    onCalculate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Indigo500, Violet600)
                        )
                    )
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Kalkulator",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Amount input
        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            label = { Text("Iznos") },
            placeholder = { Text("0.00") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Indigo500,
                unfocusedBorderColor = DarkCardBorder,
                focusedLabelColor = Indigo400,
                unfocusedLabelColor = TextMuted,
                cursorColor = Indigo500,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedPlaceholderColor = TextMuted,
                unfocusedPlaceholderColor = TextMuted
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Currency selectors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CurrencySelector(
                label = "Iz",
                selected = fromCurrency,
                currencies = currencies,
                onSelect = onFromChange,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "\u2192",
                fontSize = 20.sp,
                color = Indigo400
            )

            CurrencySelector(
                label = "U",
                selected = toCurrency,
                currencies = currencies,
                onSelect = onToChange,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Calculate button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Indigo500, Violet600)
                    )
                )
                .clickable { onCalculate() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isCalculating) "Racunam..." else "Izracunaj",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        // Result
        if (result != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Indigo500.copy(alpha = 0.1f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Rezultat konverzije",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = result,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Indigo400
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrencySelector(
    label: String,
    selected: String,
    currencies: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(DarkBg)
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                text = selected,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(DarkCard)
        ) {
            currencies.distinct().forEach { currency ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = currency,
                            color = if (currency == selected) Indigo400 else Color.White,
                            fontWeight = if (currency == selected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelect(currency)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyRatesState() {
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
            Text(text = "\uD83D\uDCB1", fontSize = 28.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Nema kursne liste",
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Kursna lista trenutno nije dostupna",
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
        repeat(5) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCard)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// ─── Helpers ───────────────────────────────────────────────

private fun formatRate(rate: Double?): String {
    if (rate == null) return "-"
    val formatter = NumberFormat.getNumberInstance(Locale("sr", "RS"))
    formatter.minimumFractionDigits = 4
    formatter.maximumFractionDigits = 4
    return formatter.format(rate)
}

private fun currencyBadgeGradient(currency: String): List<Color> {
    return when (currency.uppercase()) {
        "EUR" -> listOf(Indigo500, Violet600)
        "USD" -> listOf(Color(0xFF22C55E), Color(0xFF16A34A))
        "CHF" -> listOf(Color(0xFFEF4444), Color(0xFFDC2626))
        "GBP" -> listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED))
        "JPY" -> listOf(Color(0xFFF59E0B), Color(0xFFD97706))
        "CAD" -> listOf(Color(0xFFEC4899), Color(0xFFDB2777))
        "AUD" -> listOf(Color(0xFF14B8A6), Color(0xFF0D9488))
        "SEK", "NOK", "DKK" -> listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
        else -> listOf(Color(0xFF6B7280), Color(0xFF4B5563))
    }
}
