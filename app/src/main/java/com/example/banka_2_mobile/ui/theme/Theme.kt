package com.example.banka_2_mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Indigo500,
    onPrimary = TextWhite,
    primaryContainer = Indigo700,
    onPrimaryContainer = Indigo300,
    secondary = Violet600,
    onSecondary = TextWhite,
    secondaryContainer = Violet700,
    onSecondaryContainer = Violet400,
    tertiary = Violet500,
    background = DarkBg,
    onBackground = TextWhite,
    surface = DarkSurface,
    onSurface = TextWhite,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextMuted,
    outline = DarkCardBorder,
    error = ErrorRed,
    onError = TextWhite
)

private val LightColorScheme = lightColorScheme(
    primary = Indigo500,
    onPrimary = TextWhite,
    primaryContainer = Indigo300,
    onPrimaryContainer = Indigo700,
    secondary = Violet600,
    onSecondary = TextWhite,
    secondaryContainer = Violet400,
    onSecondaryContainer = Violet700,
    tertiary = Violet500,
    background = LightBg,
    onBackground = TextDark,
    surface = LightSurface,
    onSurface = TextDark,
    surfaceVariant = LightBg,
    onSurfaceVariant = TextMuted,
    error = ErrorRed,
    onError = TextWhite
)

@Composable
fun Banka2MobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
