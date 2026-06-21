package dev.echoapp.echo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Echo shapes — rounded and soft. Cards land around 16–20dp; pill buttons use
 * full rounding via their own component shape. See [EchoShapes].
 */
val EchoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),   // chips, inputs, small cards
    large = RoundedCornerShape(20.dp),    // post cards / sheets
    extraLarge = RoundedCornerShape(28.dp)
)
