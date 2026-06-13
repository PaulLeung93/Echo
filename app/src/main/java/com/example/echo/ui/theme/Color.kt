package com.example.echo.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Echo "Neighborhood" brand palette (light-first).
 *
 * Warm, friendly, hyperlocal. Coral is the brand/action color, teal the
 * secondary accent (links, distance badges), sunny yellow a small highlight.
 * Neutrals are warm (cream canvas, warm-white cards, warm-brown ink).
 *
 * Derived from the Stitch "Echo Neighborhood" design system.
 */

// --- Brand ---
val Coral = Color(0xFFFF6F4D)        // primary action / brand
val CoralPressed = Color(0xFFE8552F)
val CoralContainer = Color(0xFFFFDBCF)
val OnCoralContainer = Color(0xFF3A0B00)

val Teal = Color(0xFF1FB6A6)         // secondary accent
val TealDeep = Color(0xFF0E8C7E)     // teal text/links on light
val TealContainer = Color(0xFFB7F0E8) // distance-badge background
val OnTealContainer = Color(0xFF00413A)

val Sunny = Color(0xFFFFC24B)        // tertiary highlight
val SunnyDeep = Color(0xFF8A6100)
val SunnyContainer = Color(0xFFFFE6B0)
val OnSunnyContainer = Color(0xFF2A1C00)

// --- Warm neutrals ---
val Cream = Color(0xFFFFF6EF)            // app background
val WarmWhite = Color(0xFFFFFDFB)        // cards / surfaces
val WarmInk = Color(0xFF3E2E1F)          // primary text
val WarmMuted = Color(0xFF6D5C47)        // secondary text
val WarmOutline = Color(0xFF8A7861)
val WarmOutlineVariant = Color(0xFFD8C7B5)
val WarmSurfaceVariant = Color(0xFFF4E7DA) // chip / subtle fill

// Tonal surface containers (warm)
val SurfaceContainerLowest = Color(0xFFFFFFFF)
val SurfaceContainerLow = Color(0xFFFFF1E4)
val SurfaceContainer = Color(0xFFFFEBD5)
val SurfaceContainerHigh = Color(0xFFFAE5CD)
val SurfaceContainerHighest = Color(0xFFF6DFC4)

// --- Error ---
val EchoError = Color(0xFFBA1A2B)
val EchoErrorContainer = Color(0xFFFFDAD6)
val OnEchoErrorContainer = Color(0xFF410006)

// --- Dark (light-first app; provided so dark mode degrades gracefully) ---
val DarkBackground = Color(0xFF1A140E)
val DarkSurface = Color(0xFF231B13)
val DarkOnSurface = Color(0xFFEDE0D4)
val DarkOnSurfaceVariant = Color(0xFFD0BFA9)
val DarkOutline = Color(0xFF9A8874)
