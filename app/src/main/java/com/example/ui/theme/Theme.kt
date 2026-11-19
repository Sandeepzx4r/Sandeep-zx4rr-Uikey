package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val BlackSharkDarkColorScheme = darkColorScheme(
    primary = NeonPurple,
    onPrimary = TextPrimary,
    primaryContainer = CardSurfaceVariant,
    onPrimaryContainer = TextPrimary,
    secondary = ChampionGold,
    onSecondary = ObsidianBg,
    secondaryContainer = DeepGold,
    onSecondaryContainer = ObsidianBg,
    tertiary = ElectricCyan,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = DeepVioletSurface,
    onSurface = TextPrimary,
    surfaceVariant = CardSurface,
    onSurfaceVariant = TextSecondary,
    outline = OutlineViolet,
    error = CrimsonError,
    onError = TextPrimary
)

@Composable
fun BlackSharkTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BlackSharkDarkColorScheme,
        typography = Typography,
        content = content
    )
}
