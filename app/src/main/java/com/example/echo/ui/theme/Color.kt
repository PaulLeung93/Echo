package com.example.echo.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Echo "Neighborhood" brand palette (light-first).
 *
 * All values are taken verbatim from the Stitch "Echo — Neighborhood Rebrand"
 * design system (projects/11588814282991010539, namedColors / designMd).
 *
 * M3 role mapping:
 *   primary            → dark burnt-coral  (#ad3418)  — button text, icon tint
 *   primaryContainer   → coral-orange      (#fe6e4c)  — filled button bg, FAB bg
 *   secondary          → deep teal         (#006b61)  — secondary actions
 *   secondaryContainer → light teal        (#75f7e6)  — distance-badge bg
 *   tertiary           → amber             (#7d5800)  — small highlights
 *   tertiaryContainer  → sunny yellow      (#ffc24b)  — tag chips bg
 *   background/surface → warm cream        (#fff8f4)
 */

// --- Primary (coral / burnt-coral) ---
val CoralDark       = Color(0xFFAD3418) // M3 primary  — text/icon on light bg
val CoralDim        = Color(0xFF9C280C) // primary-dim (pressed tint)
val CoralOrange     = Color(0xFFFE6E4C) // primary-container / FAB fill
val CoralOrangeDim  = Color(0xFFED6241) // primary-container-dim
val OnCoral         = Color(0xFFFFF7F6) // on-primary
val OnCoralContainer = Color(0xFF400700) // on-primary-container
val InversePrimary  = Color(0xFFFE6E4C) // inverse-primary

// --- Secondary (sage green — replaces teal as M3 secondary role) ---
val SageDeep        = Color(0xFF2D5A35) // M3 secondary (dark sage)
val SageDim         = Color(0xFF224529)
val SageContainer   = Color(0xFFD6E8D0) // M3 secondary-container (pale sage, same as chip)
val SageContainerDim = Color(0xFFC2DAB9)
val OnSage          = Color(0xFFFFFFFF) // on-secondary
val OnSageContainer = Color(0xFF1E4A1A) // on-secondary-container

// --- Teal (retained as brand tokens; no longer mapped to any M3 role) ---
val TealDeep        = Color(0xFF006B61)
val TealDim         = Color(0xFF005E55)
val TealContainer   = Color(0xFF75F7E6)
val TealContainerDim = Color(0xFF66E9D8)
val OnTeal          = Color(0xFFE2FFF9)
val OnTealContainer = Color(0xFF005C53)

// --- Tertiary (sunny yellow / amber) ---
val SunnyDeep       = Color(0xFF7D5800) // M3 tertiary
val SunnyDim        = Color(0xFF6E4D00)
val SunnyContainer  = Color(0xFFFFC24B) // M3 tertiary-container
val SunnyContainerDim = Color(0xFFEFB43E)
val OnSunny         = Color(0xFFFFF8F1) // on-tertiary
val OnSunnyContainer = Color(0xFF5A3F00) // on-tertiary-container

// --- Warm neutrals ---
val Cream           = Color(0xFFFFF8F4) // background / surface
val WarmSurface     = Color(0xFFFFF8F4) // surface
val WarmSurfaceDim  = Color(0xFFEED6BC) // surface-dim
val WarmSurfaceBright = Color(0xFFFFF8F4) // surface-bright
val WarmInk         = Color(0xFF3E301E) // on-background / on-surface
val WarmMuted       = Color(0xFF6D5C47) // on-surface-variant
val WarmOutline     = Color(0xFF8A7861)
val WarmOutlineVariant = Color(0xFFC4AF96) // corrected from Stitch (#c4af96)
val WarmSurfaceVariant = Color(0xFFF6DFC4) // surface-variant (chip / subtle fill)
val InverseSurface  = Color(0xFF130D05)
val InverseOnSurface = Color(0xFFA89B8D)

// Tonal surface containers (warm) — taken from Stitch namedColors
val SurfaceContainerLowest  = Color(0xFFFFFFFF)
val SurfaceContainerLow     = Color(0xFFFFF1E4)
val SurfaceContainer        = Color(0xFFFFEBD5)
val SurfaceContainerHigh    = Color(0xFFFAE5CD)
val SurfaceContainerHighest = Color(0xFFF6DFC4)

// --- Distance badge — warm amber (user-chosen alternative to teal) ---
val WarmAmberBadge   = Color(0xFFFFE0A0) // badge background
val OnWarmAmberBadge = Color(0xFF6B4400) // badge text

// --- Tag chips — warm terracotta (rustic, organic neighborhood feel) ---
val WarmTerracottaChip   = Color(0xFFF5D5C2) // chip background
val OnWarmTerracottaChip = Color(0xFF6B2400) // chip text

// --- Error — aligned to Stitch (#a8364b / #f97386 / #6e0523) ---
val EchoError          = Color(0xFFA8364B)
val EchoErrorContainer = Color(0xFFF97386)
val OnEchoError        = Color(0xFFFFF7F7)
val OnEchoErrorContainer = Color(0xFF6E0523)

// --- Dark (warm dark-mode fallback; light-first app) ---
val DarkBackground      = Color(0xFF1A140E)
val DarkSurface         = Color(0xFF231B13)
val DarkOnSurface       = Color(0xFFEDE0D4)
val DarkOnSurfaceVariant = Color(0xFFD0BFA9)
val DarkOutline         = Color(0xFF9A8874)
