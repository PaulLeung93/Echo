package com.example.echo.ui.splash

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.echo.R

// Matches the Stitch splash easing: cubic-bezier(0.1, 0.4, 0.2, 1).
private val RippleEasing = CubicBezierEasing(0.1f, 0.4f, 0.2f, 1f)

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Logo with expanding "echo" ripples radiating from behind it.
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
                EchoRipples(modifier = Modifier.matchParentSize())
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(100.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(R.drawable.echo_logo),
                            contentDescription = null,
                            modifier = Modifier.size(76.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Echo",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Your neighborhood, in real time.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Three concentric ring outlines (coral / teal / coral-container) that expand and
 * fade outward on a staggered 3s loop — the signature Echo "ripple" from the
 * Stitch splash (scale 0.6→3.5, opacity 0.8→0, rings 1s apart).
 */
@Composable
private fun EchoRipples(modifier: Modifier = Modifier) {
    val ringColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.primaryContainer
    )
    val transition = rememberInfiniteTransition(label = "echoRipple")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 3000, easing = LinearEasing)),
        label = "t"
    )
    Box(
        modifier = modifier.drawBehind {
            val maxRadius = size.minDimension / 2f
            val baseRadius = maxRadius / 3.5f
            val strokeWidth = 4.dp.toPx()
            ringColors.forEachIndexed { i, color ->
                val phase = (t + i / 3f) % 1f
                val scale = 0.6f + (3.5f - 0.6f) * RippleEasing.transform(phase)
                val alpha = 0.8f * (1f - phase)
                drawCircle(
                    color = color.copy(alpha = alpha),
                    radius = baseRadius * scale,
                    style = Stroke(width = strokeWidth)
                )
            }
        }
    )
}
