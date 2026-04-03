package com.example.banka_2_mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = darkColorScheme(
    primary = Indigo500,
    onPrimary = TextWhite,
    primaryContainer = Indigo700,
    onPrimaryContainer = Indigo300,
    secondary = Violet600,
    onSecondary = TextWhite,
    secondaryContainer = Violet700,
    onSecondaryContainer = Violet400,
    tertiary = Emerald500,
    onTertiary = TextWhite,
    background = DarkBg,
    onBackground = TextWhite,
    surface = DarkBg,
    onSurface = TextWhite,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextMuted,
    surfaceContainerLowest = DarkBg,
    surfaceContainerLow = DarkBg,
    surfaceContainer = DarkCard,
    surfaceContainerHigh = DarkCard,
    surfaceContainerHighest = DarkCardElevated,
    outline = DarkCardBorder,
    outlineVariant = DarkBorder,
    error = Rose500,
    onError = TextWhite,
    errorContainer = DarkCard,
    onErrorContainer = Rose500,
    inverseSurface = TextWhite,
    inverseOnSurface = DarkBg,
    inversePrimary = Indigo600,
    scrim = DarkBg
)

@Composable
fun Banka2MobileTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}
