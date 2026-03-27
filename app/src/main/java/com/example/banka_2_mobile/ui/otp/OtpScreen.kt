package com.example.banka_2_mobile.ui.otp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.example.banka_2_mobile.data.model.OtpResponse
import com.example.banka_2_mobile.data.repository.AuthRepository
import com.example.banka_2_mobile.ui.theme.DarkBg
import com.example.banka_2_mobile.ui.theme.DarkCard
import com.example.banka_2_mobile.ui.theme.DarkCardBorder
import com.example.banka_2_mobile.ui.theme.Indigo400
import com.example.banka_2_mobile.ui.theme.Indigo500
import com.example.banka_2_mobile.ui.theme.SuccessGreen
import com.example.banka_2_mobile.ui.theme.TextMuted
import com.example.banka_2_mobile.ui.theme.Violet600
import com.example.banka_2_mobile.ui.theme.WarningYellow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpScreen(
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var otpData by remember { mutableStateOf<OtpResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val email = authRepository.getEmail() ?: ""
    val displayName = extractNameFromEmail(email)

    // Function to fetch OTP
    suspend fun fetchOtp() {
        try {
            val response = RetrofitClient.api.getActiveOtp()
            if (response.isSuccessful) {
                otpData = response.body()
                otpData?.expiresInSeconds?.let { countdown = it }
            } else if (response.code() == 401) {
                // Token expired and refresh failed
                authRepository.clearTokens()
                onLogout()
                return
            } else {
                otpData = OtpResponse(active = false, message = "Greška pri učitavanju")
            }
        } catch (e: Exception) {
            if (otpData == null) {
                otpData = OtpResponse(active = false, message = "Greška u mreži")
            }
            errorMessage = "Greška u mreži. Pokušavam ponovo..."
        }
        isLoading = false
        isRefreshing = false
    }

    // Show error snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    // Polling every 5 seconds
    LaunchedEffect(Unit) {
        while (true) {
            fetchOtp()
            delay(5000)
        }
    }

    // Countdown timer (decrements locally every second)
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    // Pulse animation for active OTP
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Waiting indicator pulse
    val waitingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waiting"
    )

    // Background orb animation
    val orbAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb"
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                scope.launch { fetchOtp() }
            },
            modifier = Modifier.padding(padding)
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
                        .align(Alignment.TopStart)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Indigo500.copy(alpha = 0.3f),
                                    Color.Transparent
                                ),
                                radius = 500f
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .size(350.dp)
                        .alpha(orbAlpha)
                        .align(Alignment.BottomEnd)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Violet600.copy(alpha = 0.3f),
                                    Color.Transparent
                                ),
                                radius = 500f
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp, bottom = 8.dp),
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

                        Button(
                            onClick = {
                                authRepository.clearTokens()
                                onLogout()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkCard
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Odjavi se",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Brand header
                    Text(
                        text = "BANKA 2",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            brush = Brush.linearGradient(
                                colors = listOf(Indigo400, Violet600),
                                start = Offset(0f, 0f),
                                end = Offset(250f, 0f)
                            )
                        )
                    )
                    Text(
                        text = "OTP VERIFIKACIJA",
                        fontSize = 11.sp,
                        color = TextMuted,
                        letterSpacing = 3.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    if (isLoading) {
                        // Loading state
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 60.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .alpha(waitingAlpha)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(Indigo500, Violet600)
                                        )
                                    )
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Učitavanje...",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    } else if (otpData?.active == true) {
                        // Active OTP display
                        ActiveOtpCard(
                            code = otpData?.code ?: "------",
                            countdown = countdown,
                            attempts = otpData?.attempts ?: 0,
                            maxAttempts = otpData?.maxAttempts ?: 3,
                            pulseScale = pulseScale
                        )
                    } else {
                        // No active OTP - waiting state
                        WaitingForOtp(alpha = waitingAlpha)
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // Footer
                    Text(
                        text = "BANKA 2025 \u2022 TIM 2",
                        fontSize = 11.sp,
                        color = TextMuted.copy(alpha = 0.4f),
                        letterSpacing = 2.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveOtpCard(
    code: String,
    countdown: Int,
    attempts: Int,
    maxAttempts: Int,
    pulseScale: Float
) {
    val minutes = countdown / 60
    val seconds = countdown % 60
    val timeString = "%02d:%02d".format(minutes, seconds)
    val isLow = countdown in 1..30

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(SuccessGreen.copy(alpha = 0.15f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(SuccessGreen)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Aktivan verifikacioni kod",
                color = SuccessGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // OTP Code Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .scale(pulseScale)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Indigo500, Violet600)
                    )
                )
                .padding(vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Vaš verifikacioni kod",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Format code with spaces: 123 456
                val formattedCode = if (code.length == 6) {
                    "${code.substring(0, 3)} ${code.substring(3)}"
                } else {
                    code
                }

                Text(
                    text = formattedCode,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White,
                    letterSpacing = 8.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Unesite ovaj kod na web aplikaciji",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Timer and attempts info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Timer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCard)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Preostalo vreme",
                    fontSize = 11.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = timeString,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isLow) WarningYellow else Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Attempts
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCard)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Pokušaji",
                    fontSize = 11.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$attempts / $maxAttempts",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (attempts >= maxAttempts - 1) WarningYellow else Color.White
                )
            }
        }
    }
}

@Composable
private fun WaitingForOtp(alpha: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 40.dp)
    ) {
        // Pulsing indicator
        Box(
            modifier = Modifier
                .size(80.dp)
                .alpha(alpha)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Indigo500.copy(alpha = 0.3f),
                            Violet600.copy(alpha = 0.3f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Indigo500, Violet600)
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Čekanje na verifikacioni zahtev...",
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Kada pokrenete transakciju na web aplikaciji,\nverifikacioni kod će se pojaviti ovde",
            fontSize = 13.sp,
            color = TextMuted,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Info card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkCard)
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "Kako funkcioniše?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))

                val steps = listOf(
                    "1. Pokrenite transakciju na web aplikaciji",
                    "2. Verifikacioni kod će se automatski pojaviti ovde",
                    "3. Unesite kod na web aplikaciji za potvrdu"
                )

                steps.forEach { step ->
                    Text(
                        text = step,
                        fontSize = 13.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(bottom = 6.dp),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

private fun extractNameFromEmail(email: String): String {
    val localPart = email.substringBefore("@")
    val parts = localPart.split(".")
    return parts.joinToString(" ") { part ->
        part.replaceFirstChar { it.uppercaseChar() }
    }
}
