package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Sentry Navy & Teal Brand Palette (derived from the Sentry ribbon logo)
val SentryNavy950 = Color(0xFF020B24) // Midnight navy screen background
val SentryNavy900 = Color(0xFF07143B) // Card & sheet container navy
val SentryNavy800 = Color(0xFF0E2259) // Elevated component navy
val SentryNavy700 = Color(0xFF17357A) // Surface variant
val SentryNavyBorder = Color(0xFF1C3D88) // Subtle navy border

// Backward-compatible surface aliases
val DeepSlate950 = SentryNavy950
val DeepSlate900 = SentryNavy900
val SurfaceSlate800 = SentryNavy800
val SurfaceVariantSlate700 = SentryNavy700
val BorderSlate700 = SentryNavyBorder

val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

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
