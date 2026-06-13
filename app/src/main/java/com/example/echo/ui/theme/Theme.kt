package com.example.echo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Coral,
    onPrimary = Color.White,
    primaryContainer = CoralContainer,
    onPrimaryContainer = OnCoralContainer,
    secondary = TealDeep,
    onSecondary = Color.White,
    secondaryContainer = TealContainer,
    onSecondaryContainer = OnTealContainer,
    tertiary = SunnyDeep,
    onTertiary = Color.White,
    tertiaryContainer = SunnyContainer,
    onTertiaryContainer = OnSunnyContainer,
    background = Cream,
    onBackground = WarmInk,
    surface = WarmWhite,
    onSurface = WarmInk,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = WarmMuted,
    surfaceTint = Coral,
    outline = WarmOutline,
    outlineVariant = WarmOutlineVariant,
    error = EchoError,
    onError = Color.White,
    errorContainer = EchoErrorContainer,
    onErrorContainer = OnEchoErrorContainer,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    scrim = Color.Black
)

private val DarkColors = darkColorScheme(
    primary = Coral,
    onPrimary = OnCoralContainer,
    primaryContainer = CoralPressed,
    onPrimaryContainer = Color.White,
    secondary = Teal,
    onSecondary = OnTealContainer,
    secondaryContainer = TealDeep,
    onSecondaryContainer = TealContainer,
    tertiary = Sunny,
    onTertiary = OnSunnyContainer,
    tertiaryContainer = SunnyDeep,
    onTertiaryContainer = SunnyContainer,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = Color(0xFF4A3B2C),
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceTint = Coral,
    outline = DarkOutline,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690008)
)

/**
 * Echo theme. Light-first by design — defaults to the warm light scheme even in
 * system dark mode (a proper dark theme is a later pass). Pass [darkTheme] = true
 * to opt in to the provisional dark scheme.
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
