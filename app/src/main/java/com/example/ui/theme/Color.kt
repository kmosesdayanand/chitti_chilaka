package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Fixed Sentry Navy & Teal Brand Palette (Used for fallback schemes, splash screen, and brand logo)
val SentryNavy950 = Color(0xFF020B24) // Midnight navy screen background
val SentryNavy900 = Color(0xFF07143B) // Card & sheet container navy
val SentryNavy800 = Color(0xFF0E2259) // Elevated component navy
val SentryNavy700 = Color(0xFF17357A) // Surface variant
val SentryNavyBorder = Color(0xFF1C3D88) // Subtle navy border

val SentryTealPrimary = Color(0xFF00E5D0) // Glowing vibrant teal
val SentryTealGlow = Color(0xFF01FDE9) // Neon teal spark
val SentryTealDeep = Color(0xFF0180AF) // Deep logo teal

// Semantic status colors carrying explicit meaning
val EmeraldSuccess = Color(0xFF10B981)
val RoseDanger = Color(0xFFFB7185)
val AmberWarning = Color(0xFFFBBF24)
val PurpleAccent = Color(0xFFA855F7)

// Dynamic ColorScheme Getters: Automatically bind to Material 3 / Material You system theme
val DeepSlate950: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.background

val DeepSlate900: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surface

val SurfaceSlate800: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surfaceContainerHigh

val SurfaceVariantSlate700: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surfaceVariant

val BorderSlate700: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.outlineVariant

val TextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurface

val TextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

val TextMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

val CyanPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.primary

// 12 curated vibrant colors for full user customization
val CountdownColors = listOf(
    "#00E5D0", // Sentry Luminous Teal
    "#0180AF", // Sentry Ocean Teal
    "#38BDF8", // Sky Cyan
    "#A855F7", // Neon Violet
    "#10B981", // Emerald
    "#FB7185", // Rose Pink
    "#FBBF24", // Amber Gold
    "#F97316", // Sunset Coral
    "#6366F1", // Electric Indigo
    "#EC4899", // Hot Fuchsia
    "#84CC16", // Mint Lime
    "#94A3B8"  // Steel Slate
)

// Categories for customizable tagging
val CountdownCategories = listOf("General", "Life", "Trip", "Birthday", "Work", "Holiday", "Milestone", "Fitness")

// Card appearance styles
val CardStyles = listOf(
    "modern" to "Modern Glow",
    "tinted" to "Tinted Accent",
    "minimal" to "Minimal Border"
)

// Unit display choices
val DisplayUnits = listOf(
    "all" to "Full Units (d, h, m, s)",
    "days" to "Days Only",
    "hours" to "Total Hours",
    "weeks" to "Weeks & Days"
)
