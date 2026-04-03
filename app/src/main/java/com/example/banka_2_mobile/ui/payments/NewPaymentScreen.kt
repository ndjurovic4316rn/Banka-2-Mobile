package com.example.banka_2_mobile.ui.payments

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.banka_2_mobile.data.api.RetrofitClient
import com.example.banka_2_mobile.data.model.Account
import com.example.banka_2_mobile.data.model.CreatePaymentRequest
import com.example.banka_2_mobile.data.repository.AuthRepository
import com.example.banka_2_mobile.ui.theme.DarkBg
import com.example.banka_2_mobile.ui.theme.DarkCard
import com.example.banka_2_mobile.ui.theme.DarkCardBorder
import com.example.banka_2_mobile.ui.theme.DarkCardElevated
import com.example.banka_2_mobile.ui.theme.ErrorRed
import com.example.banka_2_mobile.ui.theme.Indigo400
import com.example.banka_2_mobile.ui.theme.Indigo500
import com.example.banka_2_mobile.ui.theme.TextMuted
import com.example.banka_2_mobile.ui.theme.Violet600
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun NewPaymentScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // ── State ───────────────────────────────────────────────
    var accounts by remember { mutableStateOf<List<Account>>(emptyList()) }
    var isLoadingAccounts by remember { mutableStateOf(true) }
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    var recipientAccountNumber by remember { mutableStateOf("") }
    var recipientName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }

    var showOtpDialog by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // ── Validation ──────────────────────────────────────────
    val amountValid = amount.toDoubleOrNull()?.let { it > 0 } ?: false
    val recipientAccountValid = recipientAccountNumber.isNotBlank()
    val recipientNameValid = recipientName.isNotBlank()
    val purposeValid = purpose.isNotBlank()
    val isFormValid = selectedAccount != null && amountValid && recipientAccountValid
            && recipientNameValid && purposeValid

    // ── Load accounts ───────────────────────────────────────
    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.api.getMyAccounts()
            if (response.isSuccessful) {
                accounts = response.body() ?: emptyList()
                if (accounts.size == 1) {
                    selectedAccount = accounts.first()
                }
            } else if (response.code() == 401) {
                authRepository.clearTokens()
            } else {
                errorMessage = "Greska pri ucitavanju racuna (${response.code()})"
            }
        } catch (e: Exception) {
            errorMessage = "Greska u mrezi. Proverite konekciju."
        }
        isLoadingAccounts = false
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    // ── Styling ─────────────────────────────────────────────
    val gradientBorder = Brush.linearGradient(
        colors = listOf(Indigo500, Violet600),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
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
        unfocusedPlaceholderColor = TextMuted
    )

    val submitGradient = Brush.linearGradient(listOf(Indigo500, Violet600))

    // ── OTP Dialog ──────────────────────────────────────────
    if (showOtpDialog) {
        OtpVerificationDialog(
            otpCode = otpCode,
            onOtpChange = { if (it.length <= 6) otpCode = it },
            isSubmitting = isSubmitting,
            onDismiss = {
                showOtpDialog = false
                otpCode = ""
            },
            onConfirm = {
                if (otpCode.length != 6) return@OtpVerificationDialog
                scope.launch {
                    isSubmitting = true
                    try {
                        val request = CreatePaymentRequest(
                            fromAccountNumber = selectedAccount!!.accountNumber,
                            toAccountNumber = recipientAccountNumber.trim(),
                            amount = amount.toDouble(),
                            recipientName = recipientName.trim(),
                            paymentPurpose = purpose.trim(),
                            otpCode = otpCode.trim()
                        )
                        val response = RetrofitClient.api.createPayment(request)
                        when {
                            response.isSuccessful -> {
                                showOtpDialog = false
                                Toast.makeText(context, "Placanje uspesno", Toast.LENGTH_SHORT)
                                    .show()
                                onBack()
                            }

                            response.code() == 401 -> {
                                authRepository.clearTokens()
                            }

                            else -> {
                                errorMessage =
                                    "Greska pri placanju (${response.code()})"
                                showOtpDialog = false
                                otpCode = ""
                            }
                        }
                    } catch (e: Exception) {
                        errorMessage = "Greska u mrezi. Pokusajte ponovo."
                        showOtpDialog = false
                        otpCode = ""
                    }
                    isSubmitting = false
                }
            }
        )
    }

    // ── Main Layout ─────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ── 1. TOP BAR ─────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Nazad",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Novo placanje",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            // Gradient accent bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Indigo500.copy(alpha = 0.6f),
                                Violet600.copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Spacer(modifier = Modifier.height(24.dp))

            // ── 2. SENDER ACCOUNT DROPDOWN ─────────────────
            Text(
                text = "Racun posaljioca",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Box {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkCard)
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!isLoadingAccounts) dropdownExpanded = true
                        }
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Indigo400,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            if (isLoadingAccounts) {
                                Text(
                                    text = "Ucitavanje...",
                                    color = TextMuted,
                                    fontSize = 14.sp
                                )
                            } else if (selectedAccount != null) {
                                Column {
                                    Text(
                                        text = selectedAccount!!.name
                                            ?: selectedAccount!!.accountType ?: "Racun",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = formatAccountNumber(selectedAccount!!.accountNumber) +
                                                "  •  ${formatCurrency(selectedAccount!!.availableBalance, selectedAccount!!.currency ?: "RSD")}",
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } else {
                                Text(
                                    text = "Izaberite racun",
                                    color = TextMuted,
                                    fontSize = 14.sp
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
                }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier
                        .background(DarkCardElevated)
                ) {
                    accounts.forEach { account ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = account.name ?: account.accountType ?: "Racun",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = formatAccountNumber(account.accountNumber) +
                                                "  •  ${formatCurrency(account.availableBalance, account.currency ?: "RSD")}",
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            },
                            onClick = {
                                selectedAccount = account
                                dropdownExpanded = false
                            }
                        )
                    }
                    if (accounts.isEmpty() && !isLoadingAccounts) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Nemate dostupne racune",
                                    color = TextMuted,
                                    fontSize = 14.sp
                                )
                            },
                            onClick = { dropdownExpanded = false }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // ── 3. RECIPIENT ACCOUNT NUMBER ────────────────
            Text(
                text = "Broj racuna primaoca",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = recipientAccountNumber,
                onValueChange = { recipientAccountNumber = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("000-0000000000-00", color = TextMuted.copy(alpha = 0.5f)) },
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = textFieldColors,
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(20.dp))

            // ── 4. RECIPIENT NAME ──────────────────────────
            Text(
                text = "Ime primaoca",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = recipientName,
                onValueChange = { recipientName = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ime i prezime / naziv", color = TextMuted.copy(alpha = 0.5f)) },
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = Color.White
                ),
                colors = textFieldColors,
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(20.dp))

            // ── 5. AMOUNT ──────────────────────────────────
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
                placeholder = { Text("0.00", color = TextMuted.copy(alpha = 0.5f)) },
                textStyle = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = textFieldColors,
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                suffix = {
                    Text(
                        text = selectedAccount?.currency ?: "RSD",
                        color = TextMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                isError = amount.isNotEmpty() && !amountValid
            )
            if (amount.isNotEmpty() && !amountValid) {
                Text(
                    text = "Unesite validan iznos",
                    fontSize = 12.sp,
                    color = ErrorRed,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            // ── 6. PURPOSE ─────────────────────────────────
            Text(
                text = "Svrha placanja",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = purpose,
                onValueChange = { purpose = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Opis svrhe placanja", color = TextMuted.copy(alpha = 0.5f)) },
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = Color.White
                ),
                colors = textFieldColors,
                shape = RoundedCornerShape(14.dp),
                minLines = 2,
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(28.dp))

            // ── 7. PAYMENT SUMMARY CARD ────────────────────
            AnimatedVisibility(
                visible = isFormValid,
                enter = expandVertically(tween(300)) + fadeIn(tween(300)),
                exit = shrinkVertically(tween(300)) + fadeOut(tween(200))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = 1.dp,
                            brush = gradientBorder,
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
                                        Brush.linearGradient(
                                            colors = listOf(Indigo500, Violet600),
                                            start = Offset(0f, 0f),
                                            end = Offset(0f, Float.POSITIVE_INFINITY)
                                        )
                                    )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Rezime placanja",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        SummaryRow("Sa racuna", formatAccountNumber(selectedAccount?.accountNumber ?: ""))
                        SummaryDivider()
                        SummaryRow("Primalac", recipientName.trim())
                        SummaryDivider()
                        SummaryRow("Racun primaoca", recipientAccountNumber.trim())
                        SummaryDivider()
                        SummaryRow("Svrha", purpose.trim())

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
                                    text = "Iznos",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextMuted
                                )
                                Text(
                                    text = formatCurrency(
                                        amount.toDoubleOrNull() ?: 0.0,
                                        selectedAccount?.currency ?: "RSD"
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
            if (isFormValid) {
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── 8. SUBMIT BUTTON ───────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isFormValid) submitGradient
                        else Brush.linearGradient(
                            listOf(
                                DarkCardBorder.copy(alpha = 0.6f),
                                DarkCardBorder.copy(alpha = 0.6f)
                            )
                        )
                    )
            ) {
                Button(
                    onClick = {
                        if (!isFormValid) return@Button
                        showOtpDialog = true
                        otpCode = ""
                    },
                    modifier = Modifier.fillMaxSize(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    enabled = isFormValid,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = if (isFormValid) Color.White else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Nastavi",
                            color = if (isFormValid) Color.White else TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // Bottom spacing
            Spacer(modifier = Modifier.height(40.dp))
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
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
    val otpFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = DarkCard,
        unfocusedContainerColor = DarkCard,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = Indigo500,
        unfocusedBorderColor = DarkCardBorder,
        cursorColor = Indigo500,
        focusedPlaceholderColor = TextMuted,
        unfocusedPlaceholderColor = TextMuted
    )

    Dialog(onDismissRequest = { if (!isSubmitting) onDismiss() }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(DarkCardElevated)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(Indigo500, Violet600)),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Indigo500, Violet600),
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, Float.POSITIVE_INFINITY)
                                )
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Verifikacija",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Unesite 6-cifreni OTP kod za potvrdu placanja",
                    fontSize = 13.sp,
                    color = TextMuted,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(20.dp))

                // OTP Input
                OutlinedTextField(
                    value = otpCode,
                    onValueChange = onOtpChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("000000", color = TextMuted.copy(alpha = 0.4f)) },
                    textStyle = TextStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        letterSpacing = 8.sp
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = otpFieldColors,
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
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
                            .background(DarkCard)
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = !isSubmitting
                            ) { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Otkazi",
                            color = TextMuted,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    // Confirm
                    val confirmEnabled = otpCode.length == 6 && !isSubmitting
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (confirmEnabled) Brush.linearGradient(listOf(Indigo500, Violet600))
                                else Brush.linearGradient(
                                    listOf(
                                        DarkCardBorder.copy(alpha = 0.6f),
                                        DarkCardBorder.copy(alpha = 0.6f)
                                    )
                                )
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = confirmEnabled
                            ) { onConfirm() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Potvrdi",
                                color = if (confirmEnabled) Color.White else TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Summary helpers ────────────────────────────────────────

@Composable
private fun SummaryRow(label: String, value: String) {
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
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Composable
private fun SummaryDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(1.dp)
            .background(DarkCardBorder.copy(alpha = 0.4f))
    )
}

// ─── Utility Functions ──────────────────────────────────────

private fun formatAccountNumber(number: String): String {
    if (number.length == 18 && number.all { it.isDigit() }) {
        return "${number.substring(0, 3)}-${number.substring(3, 13)}-${number.substring(13)}"
    }
    return number
}

private fun formatCurrency(amount: Double, currency: String): String {
    val formatter = NumberFormat.getNumberInstance(Locale("sr", "RS"))
    formatter.minimumFractionDigits = 2
    formatter.maximumFractionDigits = 2
    return "${formatter.format(amount)} $currency"
}
