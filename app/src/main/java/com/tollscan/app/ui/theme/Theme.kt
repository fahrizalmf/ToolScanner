package com.tollscan.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = TollBlue40,
    onPrimary = Color.White,
    secondary = TollGold,
    onSecondary = Color.White,
    background = BackgroundLight,
    onBackground = Color(0xFF1A1C1E),
    surface = SurfaceLight,
    onSurface = Color(0xFF1A1C1E),
    error = ErrorRed
)

private val DarkColors = darkColorScheme(
    primary = TollBlue80,
    onPrimary = TollBlueDark,
    secondary = TollGoldLight,
    onSecondary = TollBlueDark,
    background = TollBlueDark,
    onBackground = Color(0xFFE3E2E6),
    surface = SurfaceDark,
    onSurface = Color(0xFFE3E2E6),
    error = Color(0xFFFFB4AB)
)

@Composable
fun TollScanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = TollTypography,
        content = content
    )
}
