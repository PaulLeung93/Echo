package com.example.echo.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A soft, pill-shaped tag chip (e.g. "#coffee"). Tinted with the secondary
 * (teal) container by default; tap to filter by the tag.
 */
@Composable
fun EchoTagChip(
    tag: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val label = if (tag.startsWith("#")) tag else "#$tag"
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = if (onClick != null) modifier.clickable { onClick() } else modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
