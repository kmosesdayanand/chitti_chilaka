package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalIsDarkTheme = compositionLocalOf { true }

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
    onBackground = Color(0xFFF8FAFC),
    surface = SentryNavy900,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = SentryNavy800,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = SentryNavyBorder,
    error = RoseDanger,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0D9488),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF134E4A),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    tertiary = Color(0xFF059669),
    background = Color(0xFFF1F5F9),
    onBackground = Color(0xFF071438),
    surface = Color.White,
    onSurface = Color(0xFF071438),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFE11D48),
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalIsDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
