package com.example.echo.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.echo.ui.theme.Coral
import com.example.echo.ui.theme.CoralPressed
import com.example.echo.ui.theme.SunnyDeep
import com.example.echo.ui.theme.Teal
import com.example.echo.ui.theme.TealDeep

private val AvatarColors = listOf(Coral, Teal, TealDeep, SunnyDeep, CoralPressed)

/**
 * A circular avatar showing the first letter of [name] on a deterministic warm
 * color. Placeholder until real profile photos exist.
 */
@Composable
fun AuthorAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val initial = name.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "?"
    val color = AvatarColors[Math.floorMod(name.hashCode(), AvatarColors.size)]
    Surface(shape = CircleShape, color = color, modifier = modifier.size(size)) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initial,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = (size.value / 2.2f).sp
            )
        }
    }
}
