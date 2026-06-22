package dev.echoapp.echo.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.echoapp.echo.R
import dev.echoapp.echo.domain.model.Comment
import dev.echoapp.echo.utils.formatTimestamp

/**
 * A single comment, Neighborhood style: a soft rounded surface with an initials
 * avatar, author + time, the message, and an optional delete action (shown only
 * for the viewer's own comments).
 */
@Composable
fun CommentCard(
    comment: Comment,
    onDelete: (() -> Unit)? = null,
    onReport: (() -> Unit)? = null,
    onBlock: (() -> Unit)? = null,
    isAuthor: Boolean = false
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        // The "AUTHOR" pill already marks the original poster, so the fill just needs a
        // faint wash rather than a solid container color.
        color = if (isAuthor) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            AuthorPhotoAvatar(
                authorId = comment.authorId,
                name = comment.username,
                fallbackPhotoUrl = comment.authorPhotoUrl,
                size = 36.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.username.substringBefore("@"),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isAuthor) {
                        Spacer(Modifier.width(6.dp))
                        AuthorBadge()
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formatTimestamp(comment.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = comment.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (onDelete != null) {
                // Own comment: delete affordance.
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = stringResource(R.string.delete_comment),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else if (onReport != null || onBlock != null) {
                // Others' comment: Report / Block menu.
                CommentOverflowMenu(onReport = onReport, onBlock = onBlock)
            }
        }
    }
}

/** "⋮" menu for someone else's comment, offering Report / Block. */
@Composable
private fun CommentOverflowMenu(onReport: (() -> Unit)?, onBlock: (() -> Unit)?) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.report_comment),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (onReport != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.report)) },
                    onClick = { expanded = false; onReport() }
                )
            }
            if (onBlock != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.block_user)) },
                    onClick = { expanded = false; onBlock() }
                )
            }
        }
    }
}

/** Small coral "AUTHOR" pill marking the original poster's own comments. */
@Composable
private fun AuthorBadge() {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primary
    ) {
        Text(
            text = "AUTHOR",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
        )
    }
}
