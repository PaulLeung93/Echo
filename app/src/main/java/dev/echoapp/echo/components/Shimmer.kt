package dev.echoapp.echo.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val SHIMMER_WIDTH = 400f

/**
 * An animated left-to-right shimmer brush in a theme-aware neutral tone. Shared
 * by the skeleton placeholders so every loading state shimmers identically.
 */
@Composable
fun rememberShimmerBrush(): Brush {
    val base = MaterialTheme.colorScheme.onSurface
    val colors = listOf(
        base.copy(alpha = 0.07f),
        base.copy(alpha = 0.17f),
        base.copy(alpha = 0.07f)
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -SHIMMER_WIDTH,
        targetValue = SHIMMER_WIDTH * 2,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing)
        ),
        label = "shimmerX"
    )
    return Brush.linearGradient(
        colors = colors,
        start = Offset(x, 0f),
        end = Offset(x + SHIMMER_WIDTH, 0f)
    )
}

/** A single rounded shimmer bar; either a fixed [width] or a [fraction] of the row. */
@Composable
fun ShimmerBar(brush: Brush, height: Dp, width: Dp? = null, fraction: Float = 1f) {
    Box(
        modifier = (if (width != null) Modifier.width(width) else Modifier.fillMaxWidth(fraction))
            .height(height)
            .clip(RoundedCornerShape(4.dp))
            .background(brush)
    )
}
