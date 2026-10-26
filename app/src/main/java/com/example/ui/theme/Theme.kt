package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SentryTealPrimary,
    onPrimary = SentryNavy950,
    primaryContainer = SentryTealDeep,
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = SentryTealGlow,
    onSecondary = SentryNavy950,
    secondaryContainer = SentryNavy800,
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = EmeraldSuccess,
    onTertiary = SentryNavy950,
    background = SentryNavy950,
    onBackground = TextPrimary,
    surface = SentryNavy900,
    onSurface = TextPrimary,
    surfaceVariant = SentryNavy800,
    onSurfaceVariant = TextSecondary,
    outline = SentryNavyBorder,
    error = RoseDanger,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0D9488),
    onPrimary = Color.White,
    secondary = Color(0xFF0369A1),
    tertiary = Color(0xFF059669),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF071438),
    surface = Color.White,
    onSurface = Color(0xFF071438),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Dark theme by default as requested
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
