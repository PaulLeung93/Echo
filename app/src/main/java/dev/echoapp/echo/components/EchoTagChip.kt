package dev.echoapp.echo.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.echoapp.echo.ui.theme.OnWarmTerracottaChip
import dev.echoapp.echo.ui.theme.WarmTerracottaChip

/**
 *  Tinted with warm terracotta; tap to filter by the tag.
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
        color = WarmTerracottaChip,
        contentColor = OnWarmTerracottaChip,
        modifier = if (onClick != null) modifier.clickable { onClick() } else modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
