package com.example.echo.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

val NavyBlue = Color(0xFF001F54)
val Navy2 = Color(0xFF224580)
val lightBlue = Color(0xFF94d4f2)
val skyBlue = Color(0xffbcf2f8)
val blue = Color(0xFF19799c)


private val DarkColorScheme = darkColorScheme(
    primary = lightBlue,
    secondary = NavyBlue,
    tertiary = blue,
    onPrimary = NavyBlue
)

private val LightColorScheme = lightColorScheme(
    primary = Navy2,
    secondary = lightBlue,
    tertiary = blue,
    onPrimary = Color.White

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun EchoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}