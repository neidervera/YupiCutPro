package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val YupiCutGreenBlackColorScheme = darkColorScheme(
    primary = CyberGreen,
    onPrimary = BackgroundDark,
    primaryContainer = Color(0xFF0A3A20),
    onPrimaryContainer = Color.White,
    secondary = NeonGreen,
    onSecondary = BackgroundDark,
    secondaryContainer = SurfaceElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = PlayheadColor,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
    error = DangerRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = YupiCutGreenBlackColorScheme,
        typography = Typography,
        content = content
    )
}
