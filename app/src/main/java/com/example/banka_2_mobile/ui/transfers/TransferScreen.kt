package com.example.banka_2_mobile.ui.transfers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.banka_2_mobile.data.api.RetrofitClient
import com.example.banka_2_mobile.data.model.Account
import com.example.banka_2_mobile.data.model.CreateTransferRequest
import com.example.banka_2_mobile.data.repository.AuthRepository
import com.example.banka_2_mobile.ui.theme.DarkBg
import com.example.banka_2_mobile.ui.theme.DarkCard
import com.example.banka_2_mobile.ui.theme.DarkCardBorder
import com.example.banka_2_mobile.ui.theme.DarkCardElevated
import com.example.banka_2_mobile.ui.theme.Indigo400
import com.example.banka_2_mobile.ui.theme.Indigo500
import com.example.banka_2_mobile.ui.theme.TextMuted
import com.example.banka_2_mobile.ui.theme.Violet600
import com.example.banka_2_mobile.ui.theme.ErrorRed
import com.example.banka_2_mobile.ui.theme.SuccessGreen
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var accounts by remember { mutableStateOf<List<Account>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Form state
    var fromAccount by remember { mutableStateOf<Account?>(null) }
    var toAccount by remember { mutableStateOf<Account?>(null) }
    var amount by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    // OTP dialog
    var showOtpDialog by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }

    // Exchange rate info
    var exchangeInfo by remember { mutableStateOf<String?>(null) }
    var isCalculatingRate by remember { mutableStateOf(false) }

    // Dropdown state
    var fromDropdownExpanded by remember { mutableStateOf(false) }
    var toDropdownExpanded by remember { mutableStateOf(false) }

    // Available "to" accounts: exclude the currently selected "from" account
    val toAccounts = accounts.filter { it.id != fromAccount?.id }

    // Validation
    val amountValue = amount.replace(",", ".").toDoubleOrNull()
    val amountValid = amountValue != null && amountValue > 0
    val fromSelected = fromAccount != null
    val toSelected = toAccount != null
    val sufficientBalance = amountValue != null && fromAccount != null &&
            amountValue <= fromAccount!!.availableBalance
    val isFormValid = fromSelected && toSelected && amountValid && sufficientBalance

    // Check if currencies differ
    val currenciesDiffer = fromAccount != null && toAccount != null &&
            (fromAccount!!.currency ?: "RSD").uppercase() !=
            (toAccount!!.currency ?: "RSD").uppercase()

    // Fetch exchange rate when currencies differ and amount is entered
    LaunchedEffect(fromAccount, toAccount, amount) {
        if (currenciesDiffer && amountValid && amountValue!! > 0) {
            isCalculatingRate = true
            try {
                val fromCurr = (fromAccount!!.currency ?: "RSD").uppercase()
                val toCurr = (toAccount!!.currency ?: "RSD").uppercase()
                val response = RetrofitClient.api.calculateExchange(
                    amount = amountValue,
                    fromCurrency = fromCurr,
                    toCurrency = toCurr
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val formatter = NumberFormat.getNumberInstance(Locale("sr", "RS"))
                        formatter.minimumFractionDigits = 2
                        formatter.maximumFractionDigits = 2
                        exchangeInfo = "Kurs: 1 $fromCurr = ${formatter.format(body.exchangeRate)} $toCurr\n" +
                                "Primate: ${formatter.format(body.convertedAmount)} $toCurr"
                    }
                } else {
                    exchangeInfo = null
                }
            } catch (_: Exception) {
                exchangeInfo = null
            }
            isCalculatingRate = false
        } else {
            exchangeInfo = null
        }
    }

    // Load accounts
    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.api.getMyAccounts()
            if (response.isSuccessful) {
                accounts = response.body() ?: emptyList()
            } else if (response.code() == 401) {
                authRepository.clearTokens()
                onLogout()
                return@LaunchedEffect
            } else {
                errorMessage = "Greska pri ucitavanju racuna (${response.code()})"
            }
        } catch (_: Exception) {
            errorMessage = "Greska u mrezi. Proverite konekciju."
        }
        isLoading = false
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    // Reset toAccount if it matches the newly selected fromAccount
    LaunchedEffect(fromAccount) {
        if (toAccount != null && toAccount!!.id == fromAccount?.id) {
            toAccount = null
        }
    }

    // Background animation
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

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = DarkCard,
        unfocusedContainerColor = DarkCard,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = Indigo500,
        unfocusedBorderColor = DarkCardBorder,
        cursorColor = Indigo500,
        focusedPlaceholderColor = TextMuted,
        unfocusedPlaceholderColor = TextMuted,
        focusedLabelColor = Indigo400,
        unfocusedLabelColor = TextMuted
    )

    // OTP Dialog
    if (showOtpDialog) {
        OtpVerificationDialog(
            otpCode = otpCode,
            onOtpChange = { otpCode = it },
            isSubmitting = isSubmitting,
            onDismiss = {
                showOtpDialog = false
                otpCode = ""
            },
            onConfirm = {
                if (otpCode.length != 6) {
                    errorMessage = "OTP kod mora imati 6 cifara"
                    return@OtpVerificationDialog
                }
                scope.launch {
                    isSubmitting = true
                    try {
                        val request = CreateTransferRequest(
                            fromAccountNumber = fromAccount!!.accountNumber,
                            toAccountNumber = toAccount!!.accountNumber,
                            amount = amountValue!!,
                            otpCode = otpCode
                        )
                        val response = RetrofitClient.api.createTransfer(request)
                        when {
                            response.isSuccessful -> {
                                showOtpDialog = false
                                otpCode = ""
                                snackbarHostState.showSnackbar("Prenos uspesan")
                                onBack()
                            }
                            response.code() == 401 -> {
                                authRepository.clearTokens()
                                onLogout()
                            }
                            else -> {
                                errorMessage = "Greska pri prenosu (${response.code()})"
                            }
                        }
                    } catch (_: Exception) {
                        errorMessage = "Greska u mrezi. Pokusajte ponovo."
                    }
                    isSubmitting = false
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
        ) {
            // Background orbs
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .alpha(orbAlpha)
                    .align(Alignment.TopEnd)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Indigo500.copy(alpha = 0.3f), Color.Transparent),
                            radius = 400f
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .alpha(orbAlpha * 0.6f)
                    .align(Alignment.BottomStart)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Violet600.copy(alpha = 0.2f), Color.Transparent),
                            radius = 350f
                        )
                    )
            )

            if (isLoading) {
                TransferLoadingShimmer()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Header ────────────────────────────────────────────
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
                                .height(28.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Indigo500, Violet600)
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Filled.SwapHoriz,
                            contentDescription = null,
                            tint = Indigo400,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Prenos izmedju racuna",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Prebacite sredstva izmedju svojih racuna",
                        fontSize = 13.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(start = 52.dp)
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    if (accounts.size < 2) {
                        // Not enough accounts for transfer
                        EmptyTransferState()
                    } else {
                        // ── From Account ──────────────────────────────────
                        Text(
                            text = "Sa racuna",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        AccountDropdown(
                            selectedAccount = fromAccount,
                            accounts = accounts,
                            expanded = fromDropdownExpanded,
                            onExpandChange = { fromDropdownExpanded = it },
                            onSelect = {
                                fromAccount = it
                                fromDropdownExpanded = false
                            },
                            placeholder = "Izaberite racun za slanje"
                        )

                        // Available balance of "from" account
                        AnimatedVisibility(
                            visible = fromAccount != null,
                            enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                            exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
                        ) {
                            fromAccount?.let { acc ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp, start = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(SuccessGreen)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Raspolozivo: ${formatCurrency(acc.availableBalance, (acc.currency ?: "RSD").uppercase())}",
                                        fontSize = 12.sp,
                                        color = SuccessGreen,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // ── Swap Icon ─────────────────────────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Indigo500.copy(alpha = 0.2f),
                                                Violet600.copy(alpha = 0.2f)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = DarkCardBorder,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        // Swap accounts
                                        val temp = fromAccount
                                        fromAccount = toAccount
                                        toAccount = temp
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SwapHoriz,
                                    contentDescription = "Zameni racune",
                                    tint = Indigo400,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // ── To Account ────────────────────────────────────
                        Text(
                            text = "Na racun",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        AccountDropdown(
                            selectedAccount = toAccount,
                            accounts = toAccounts,
                            expanded = toDropdownExpanded,
                            onExpandChange = { toDropdownExpanded = it },
                            onSelect = {
                                toAccount = it
                                toDropdownExpanded = false
                            },
                            placeholder = "Izaberite racun za prijem"
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // ── Amount Input ──────────────────────────────────
                        Text(
                            text = "Iznos",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    "0.00",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 20.sp
                                )
                            },
                            suffix = {
                                fromAccount?.let {
                                    Text(
                                        text = (it.currency ?: "RSD").uppercase(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Indigo400
                                    )
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            ),
                            colors = textFieldColors,
                            shape = RoundedCornerShape(14.dp),
                            isError = amount.isNotEmpty() && !amountValid ||
                                    (amountValid && fromAccount != null && !sufficientBalance)
                        )

                        // Validation messages
                        if (amount.isNotEmpty() && amountValid && fromAccount != null && !sufficientBalance) {
                            Text(
                                text = "Nedovoljno sredstava na racunu",
                                fontSize = 12.sp,
                                color = ErrorRed,
                                modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                            )
                        }

                        // ── Exchange Rate Info ────────────────────────────
                        AnimatedVisibility(
                            visible = currenciesDiffer && (exchangeInfo != null || isCalculatingRate),
                            enter = expandVertically(tween(300)) + fadeIn(tween(300)),
                            exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Indigo500.copy(alpha = 0.08f),
                                                Violet600.copy(alpha = 0.12f)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = Indigo500.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = null,
                                        tint = Indigo400,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    if (isCalculatingRate) {
                                        Text(
                                            text = "Racunam kurs...",
                                            fontSize = 13.sp,
                                            color = TextMuted,
                                            fontWeight = FontWeight.Medium
                                        )
                                    } else {
                                        Text(
                                            text = exchangeInfo ?: "",
                                            fontSize = 13.sp,
                                            color = Indigo400,
                                            fontWeight = FontWeight.Medium,
                                            lineHeight = 20.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // ── Summary Card ──────────────────────────────────
                        AnimatedVisibility(
                            visible = fromSelected && toSelected && amountValid,
                            enter = expandVertically(tween(300)) + fadeIn(tween(300)),
                            exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Indigo500.copy(alpha = 0.5f),
                                                Violet600.copy(alpha = 0.5f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .background(DarkCard)
                                    .padding(20.dp)
                            ) {
                                Column {
                                    // Header
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(20.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(Indigo500, Violet600)
                                                    )
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Rezime prenosa",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))

                                    SummaryRow(
                                        "Sa racuna",
                                        formatAccountNumber(fromAccount?.accountNumber ?: "")
                                    )
                                    SummaryDivider()
                                    SummaryRow(
                                        "Na racun",
                                        formatAccountNumber(toAccount?.accountNumber ?: "")
                                    )
                                    SummaryDivider()
                                    SummaryRow(
                                        "Iznos",
                                        "${formatCurrency(amountValue ?: 0.0, (fromAccount?.currency ?: "RSD").uppercase())}"
                                    )

                                    if (currenciesDiffer && exchangeInfo != null) {
                                        SummaryDivider()
                                        SummaryRow(
                                            "Valutna konverzija",
                                            "${(fromAccount?.currency ?: "RSD").uppercase()} \u2192 ${(toAccount?.currency ?: "RSD").uppercase()}",
                                            valueColor = Indigo400
                                        )
                                    }

                                    // Total row
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(DarkCardElevated)
                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Ukupno za prenos",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextMuted
                                            )
                                            Text(
                                                text = formatCurrency(
                                                    amountValue ?: 0.0,
                                                    (fromAccount?.currency ?: "RSD").uppercase()
                                                ),
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = Indigo400
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // ── Submit Button ─────────────────────────────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isFormValid)
                                        Brush.linearGradient(listOf(Indigo500, Violet600))
                                    else
                                        Brush.linearGradient(
                                            listOf(
                                                DarkCardBorder.copy(alpha = 0.6f),
                                                DarkCardBorder.copy(alpha = 0.6f)
                                            )
                                        )
                                )
                                .clickable(enabled = isFormValid) {
                                    showOtpDialog = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nastavi",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFormValid) Color.White else TextMuted,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(40.dp))
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

// ─── Account Dropdown Selector ──────────────────────────────

@Composable
private fun AccountDropdown(
    selectedAccount: Account?,
    accounts: List<Account>,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onSelect: (Account) -> Unit,
    placeholder: String
) {
    Box {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DarkCard)
                .border(
                    width = 1.dp,
                    color = if (expanded) Indigo500 else DarkCardBorder,
                    shape = RoundedCornerShape(14.dp)
                )
                .clickable { onExpandChange(!expanded) }
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            if (selectedAccount != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Currency badge
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.linearGradient(
                                        currencyGradient((selectedAccount.currency ?: "RSD").uppercase())
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currencySymbol((selectedAccount.currency ?: "RSD").uppercase()),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = selectedAccount.name ?: selectedAccount.accountType ?: "Racun",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = formatAccountNumber(selectedAccount.accountNumber),
                                fontSize = 11.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = placeholder,
                        fontSize = 14.sp,
                        color = TextMuted
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandChange(false) },
            modifier = Modifier
                .background(DarkCard)
                .fillMaxWidth(0.9f)
        ) {
            accounts.forEach { account ->
                val currency = (account.currency ?: "RSD").uppercase()
                val isSelected = selectedAccount?.id == account.id
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.linearGradient(currencyGradient(currency))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currencySymbol(currency),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = account.name ?: account.accountType ?: "Racun",
                                    color = if (isSelected) Indigo400 else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formatAccountNumber(account.accountNumber),
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = formatCurrency(account.availableBalance, currency),
                                fontSize = 12.sp,
                                color = if (isSelected) Indigo400 else TextMuted,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    },
                    onClick = { onSelect(account) }
                )
            }
        }
    }
}

// ─── OTP Verification Dialog ────────────────────────────────

@Composable
private fun OtpVerificationDialog(
    otpCode: String,
    onOtpChange: (String) -> Unit,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = { if (!isSubmitting) onDismiss() }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(DarkCard)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Indigo500.copy(alpha = 0.5f),
                            Violet600.copy(alpha = 0.5f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Indigo500.copy(alpha = 0.15f),
                                    Violet600.copy(alpha = 0.15f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = Indigo500.copy(alpha = 0.3f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\uD83D\uDD12",
                        fontSize = 24.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Potvrda prenosa",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Unesite 6-cifreni OTP kod za potvrdu prenosa",
                    fontSize = 13.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // OTP Input
                OutlinedTextField(
                    value = otpCode,
                    onValueChange = {
                        if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                            onOtpChange(it)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "000000",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 24.sp
                        )
                    },
                    textStyle = TextStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        color = Color.White,
                        letterSpacing = 8.sp
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkBg,
                        unfocusedContainerColor = DarkBg,
                        focusedBorderColor = Indigo500,
                        unfocusedBorderColor = DarkCardBorder,
                        cursorColor = Indigo500,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedPlaceholderColor = TextMuted.copy(alpha = 0.3f),
                        unfocusedPlaceholderColor = TextMuted.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkCardElevated)
                            .border(
                                width = 1.dp,
                                color = DarkCardBorder,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable(enabled = !isSubmitting) { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Otkazi",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted
                        )
                    }

                    // Confirm
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (otpCode.length == 6 && !isSubmitting)
                                    Brush.linearGradient(listOf(Indigo500, Violet600))
                                else
                                    Brush.linearGradient(
                                        listOf(
                                            DarkCardBorder.copy(alpha = 0.6f),
                                            DarkCardBorder.copy(alpha = 0.6f)
                                        )
                                    )
                            )
                            .clickable(enabled = otpCode.length == 6 && !isSubmitting) { onConfirm() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Potvrdi",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (otpCode.length == 6) Color.White else TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Empty State ────────────────────────────────────────────

@Composable
private fun EmptyTransferState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Indigo500.copy(alpha = 0.15f),
                            Violet600.copy(alpha = 0.15f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = DarkCardBorder,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.AccountBalance,
                contentDescription = null,
                tint = Indigo400,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Nedovoljno racuna",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Potrebna su najmanje dva racuna za prenos sredstava",
            fontSize = 14.sp,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

// ─── Summary Components ─────────────────────────────────────

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextMuted,
            fontWeight = FontWeight.Normal
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = valueColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SummaryDivider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        repeat(40) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(1.dp)
                    .background(DarkCardBorder.copy(alpha = 0.6f))
            )
        }
    }
}

// ─── Loading Shimmer ────────────────────────────────────────

@Composable
private fun TransferLoadingShimmer() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Header shimmer
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .alpha(shimmerAlpha)
                    .background(DarkCard)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .alpha(shimmerAlpha)
                    .background(DarkCardBorder)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .alpha(shimmerAlpha)
                    .background(DarkCard)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .alpha(shimmerAlpha)
                    .background(DarkCard)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .padding(start = 52.dp)
                .width(220.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .alpha(shimmerAlpha)
                .background(DarkCard)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // "Sa racuna" label shimmer
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .alpha(shimmerAlpha)
                .background(DarkCard)
        )
        Spacer(modifier = Modifier.height(10.dp))
        // From dropdown shimmer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(14.dp))
                .alpha(shimmerAlpha)
                .background(DarkCard)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Swap icon shimmer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .alpha(shimmerAlpha)
                    .background(DarkCard)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // "Na racun" label shimmer
        Box(
            modifier = Modifier
                .width(70.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .alpha(shimmerAlpha)
                .background(DarkCard)
        )
        Spacer(modifier = Modifier.height(10.dp))
        // To dropdown shimmer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(14.dp))
                .alpha(shimmerAlpha)
                .background(DarkCard)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Amount label shimmer
        Box(
            modifier = Modifier
                .width(50.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .alpha(shimmerAlpha)
                .background(DarkCard)
        )
        Spacer(modifier = Modifier.height(10.dp))
        // Amount input shimmer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(14.dp))
                .alpha(shimmerAlpha)
                .background(DarkCard)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Button shimmer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .alpha(shimmerAlpha)
                .background(DarkCard)
        )
    }
}

// ─── Utility Functions ──────────────────────────────────────

private fun formatCurrency(amount: Double, currency: String): String {
    val formatter = NumberFormat.getNumberInstance(Locale("sr", "RS"))
    formatter.minimumFractionDigits = 2
    formatter.maximumFractionDigits = 2
    return "${formatter.format(amount)} $currency"
}

private fun formatAccountNumber(number: String): String {
    if (number.length == 18 && number.all { it.isDigit() }) {
        return "${number.substring(0, 3)}-${number.substring(3, 13)}-${number.substring(13)}"
    }
    return number
}

private fun currencySymbol(currency: String): String {
    return when (currency.uppercase()) {
        "RSD" -> "din"
        "EUR" -> "\u20AC"
        "USD" -> "$"
        "CHF" -> "Fr"
        "GBP" -> "\u00A3"
        "JPY" -> "\u00A5"
        "CAD" -> "C$"
        "AUD" -> "A$"
        else -> currency.take(2)
    }
}

private fun currencyGradient(currency: String): List<Color> {
    return when (currency.uppercase()) {
        "RSD" -> listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
        "EUR" -> listOf(Indigo500, Violet600)
        "USD" -> listOf(Color(0xFF22C55E), Color(0xFF16A34A))
        "CHF" -> listOf(Color(0xFFEF4444), Color(0xFFDC2626))
        "GBP" -> listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED))
        "JPY" -> listOf(Color(0xFFF59E0B), Color(0xFFD97706))
        "CAD" -> listOf(Color(0xFFEC4899), Color(0xFFDB2777))
        "AUD" -> listOf(Color(0xFF14B8A6), Color(0xFF0D9488))
        else -> listOf(Color(0xFF6B7280), Color(0xFF4B5563))
    }
}
