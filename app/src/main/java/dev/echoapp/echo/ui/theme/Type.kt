package dev.echoapp.echo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.echoapp.echo.R

/**
 * Echo type system: Newsreader (warm editorial serif) for display/headline/large
 * titles, Plus Jakarta Sans (warm modern sans) for everything else. Bundled as
 * variable TTFs in res/font (weights applied via the font's weight axis).
 */
val Newsreader = FontFamily(
    Font(R.font.newsreader, weight = FontWeight.Medium),
    Font(R.font.newsreader, weight = FontWeight.SemiBold),
    Font(R.font.newsreader, weight = FontWeight.Bold)
)

val Jakarta = FontFamily(
    Font(R.font.plus_jakarta_sans, weight = FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, weight = FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, weight = FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, weight = FontWeight.Bold)
)

val Typography = Typography(
    // Display, headline & large title — Newsreader serif
    displayLarge = TextStyle(
        fontFamily = Newsreader, fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp, lineHeight = 48.sp, letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = Newsreader, fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = (-0.25).sp
    ),
    displaySmall = TextStyle(
        fontFamily = Newsreader, fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp, lineHeight = 36.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = Newsreader, fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp, lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Newsreader, fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, lineHeight = 30.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Newsreader, fontWeight = FontWeight.Medium,
        fontSize = 20.sp, lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Newsreader, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, lineHeight = 28.sp
    ),
    // Small/medium titles, body, labels — Plus Jakarta Sans
    titleMedium = TextStyle(
        fontFamily = Jakarta, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Jakarta, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Jakarta, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Jakarta, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Jakarta, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Jakarta, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Jakarta, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Jakarta, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp
    )
)
