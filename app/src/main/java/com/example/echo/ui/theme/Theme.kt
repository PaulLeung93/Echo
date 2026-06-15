package com.example.echo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * M3 color scheme derived from the Stitch "Echo — Neighborhood Rebrand"
 * design system (projects/11588814282991010539).
 *
 * Light scheme uses Stitch's namedColors verbatim.
 * Dark scheme is a provisional warm-dark fallback.
 */
private val LightColors = lightColorScheme(
    primary                = CoralDark,            // #ad3418 — button text/icon
    onPrimary              = OnCoral,              // #fff7f6
    primaryContainer       = CoralOrange,          // #fe6e4c — FAB/filled-button bg
    onPrimaryContainer     = OnCoralContainer,     // #400700
    secondary              = TealDeep,             // #006b61
    onSecondary            = OnTeal,               // #e2fff9
    secondaryContainer     = TealContainer,        // #75f7e6 — distance-badge bg
    onSecondaryContainer   = OnTealContainer,      // #005c53
    tertiary               = SunnyDeep,            // #7d5800
    onTertiary             = OnSunny,              // #fff8f1
    tertiaryContainer      = SunnyContainer,       // #ffc24b — tag-chip bg
    onTertiaryContainer    = OnSunnyContainer,     // #5a3f00
    background             = Cream,                // #fff8f4
    onBackground           = WarmInk,              // #3e301e
    surface                = WarmSurface,          // #fff8f4
    onSurface              = WarmInk,              // #3e301e
    surfaceVariant         = WarmSurfaceVariant,   // #f6dfc4
    onSurfaceVariant       = WarmMuted,            // #6d5c47
    surfaceTint            = CoralDark,            // #ad3418
    outline                = WarmOutline,          // #8a7861
    outlineVariant         = WarmOutlineVariant,   // #c4af96
    error                  = EchoError,            // #a8364b
    onError                = OnEchoError,          // #fff7f7
    errorContainer         = EchoErrorContainer,   // #f97386
    onErrorContainer       = OnEchoErrorContainer, // #6e0523
    inverseSurface         = InverseSurface,       // #130d05
    inverseOnSurface       = InverseOnSurface,     // #a89b8d
    inversePrimary         = InversePrimary,       // #fe6e4c
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow    = SurfaceContainerLow,
    surfaceContainer       = SurfaceContainer,
    surfaceContainerHigh   = SurfaceContainerHigh,
    surfaceContainerHighest= SurfaceContainerHighest,
    scrim                  = Color.Black
)

private val DarkColors = darkColorScheme(
    primary                = CoralOrange,          // coral-orange reads well on dark
    onPrimary              = OnCoralContainer,
    primaryContainer       = CoralDim,
    onPrimaryContainer     = OnCoral,
    secondary              = TealContainer,        // light teal on dark
    onSecondary            = OnTealContainer,
    secondaryContainer     = TealDim,
    onSecondaryContainer   = TealContainerDim,
    tertiary               = SunnyContainer,       // sunny yellow on dark
    onTertiary             = OnSunnyContainer,
    tertiaryContainer      = SunnyDim,
    onTertiaryContainer    = SunnyContainerDim,
    background             = DarkBackground,
    onBackground           = DarkOnSurface,
    surface                = DarkSurface,
    onSurface              = DarkOnSurface,
    surfaceVariant         = Color(0xFF4A3B2C),
    onSurfaceVariant       = DarkOnSurfaceVariant,
    surfaceTint            = CoralOrange,
    outline                = DarkOutline,
    error                  = Color(0xFFFFB4AB),
    onError                = Color(0xFF690008)
)

/**
 * Echo theme. Light-first by design — defaults to the warm light scheme.
 * Pass [darkTheme] = true to opt in to the provisional dark scheme
 * (persisted via Jetpack DataStore in SettingsViewModel).
 */
@Composable
fun EchoTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = EchoShapes,
        content = content
    )
}
