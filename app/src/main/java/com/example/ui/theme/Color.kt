package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Sentry Navy & Teal Brand Palette
val SentryNavy950 = Color(0xFF020B24) // Midnight navy screen background
val SentryNavy900 = Color(0xFF07143B) // Card & sheet container navy
val SentryNavy800 = Color(0xFF0E2259) // Elevated component navy
val SentryNavy700 = Color(0xFF17357A) // Surface variant
val SentryNavyBorder = Color(0xFF1C3D88) // Subtle navy border

// Adaptive surface and text colors that automatically follow system/app theme
val DeepSlate950: Color
    @Composable
    get() = if (LocalIsDarkTheme.current) SentryNavy950 else Color(0xFFF1F5F9)

val DeepSlate900: Color
    @Composable
    get() = if (LocalIsDarkTheme.current) SentryNavy900 else Color(0xFFFFFFFF)

val SurfaceSlate800: Color
    @Composable
    get() = if (LocalIsDarkTheme.current) SentryNavy800 else Color(0xFFE2E8F0)

val SurfaceVariantSlate700: Color
    @Composable
    get() = if (LocalIsDarkTheme.current) SentryNavy700 else Color(0xFFCBD5E1)

val BorderSlate700: Color
    @Composable
    get() = if (LocalIsDarkTheme.current) SentryNavyBorder else Color(0xFFE2E8F0)

val TextPrimary: Color
    @Composable
    get() = if (LocalIsDarkTheme.current) Color(0xFFF8FAFC) else Color(0xFF0F172A)

val TextSecondary: Color
    @Composable
    get() = if (LocalIsDarkTheme.current) Color(0xFF94A3B8) else Color(0xFF475569)

val TextMuted: Color
    @Composable
    get() = if (LocalIsDarkTheme.current) Color(0xFF64748B) else Color(0xFF94A3B8)

// Brand Teals (Luminous Sentry glow and ribbon accents)
val SentryTealPrimary = Color(0xFF00E5D0) // Glowing vibrant teal
val SentryTealGlow = Color(0xFF01FDE9) // Neon teal spark
val SentryTealDeep = Color(0xFF0180AF) // Deep logo teal

// Primary Accent
val CyanPrimary = SentryTealPrimary
val PurpleAccent = Color(0xFFA855F7)
val EmeraldSuccess = Color(0xFF10B981)
val RoseDanger = Color(0xFFFB7185)
val AmberWarning = Color(0xFFFBBF24)

// 12 curated vibrant colors for full customization
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
