package com.example.stockassistantmcpdemo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ForestPrimaryDark,
    onPrimary = NightBackground,
    primaryContainer = NightSurfaceVariant,
    onPrimaryContainer = ForestPrimaryDark,
    secondary = ClaySecondaryDark,
    onSecondary = NightBackground,
    tertiary = AmberTertiaryDark,
    background = NightBackground,
    onBackground = WarmSurface,
    surface = NightSurface,
    onSurface = WarmSurface,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = ForestPrimaryDark,
    outline = NightOutline
)

private val LightColorScheme = lightColorScheme(
    primary = ForestPrimary,
    onPrimary = WarmSurface,
    primaryContainer = ForestPrimaryContainer,
    onPrimaryContainer = InkText,
    secondary = ClaySecondary,
    onSecondary = WarmSurface,
    secondaryContainer = ClaySecondaryContainer,
    onSecondaryContainer = InkText,
    tertiary = AmberTertiary,
    onTertiary = WarmSurface,
    background = CreamBackground,
    onBackground = InkText,
    surface = WarmSurface,
    onSurface = InkText,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = InkText,
    outline = WarmOutline
)

@Composable
fun StockAssistantMCPDemoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
