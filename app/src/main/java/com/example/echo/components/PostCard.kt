package com.example.echo.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.echo.domain.model.Post
import com.example.echo.ui.theme.EchoTheme
import com.example.echo.ui.theme.WarmAmberBadge
import com.example.echo.ui.theme.OnWarmAmberBadge
import com.example.echo.utils.formatTimestamp
import androidx.compose.ui.tooling.preview.Preview

/**
 * A single post (an "echo") in the Neighborhood style: warm-white rounded card,
 * an initials avatar with author + time, the message, soft tag chips, and a
 * heart-style like + comment footer.
 */
@Composable
fun PostCard(
    post: Post,
    isLiked: Boolean,
    likeCount: Int,
    commentCount: Int,
    onLikeClick: () -> Unit,
    onClick: () -> Unit,
    onTagClick: ((String) -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    distanceLabel: String? = null,
    modifier: Modifier = Modifier
) {
    // Drives the heart "pop" on tap.
    val likeScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Author header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AuthorAvatar(name = post.username, size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.username.substringBefore("@"),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (distanceLabel != null) {
                        Spacer(Modifier.height(2.dp))
                        Surface(
                            shape = CircleShape,
                            color = WarmAmberBadge,
                            contentColor = OnWarmAmberBadge
                        ) {
                            Text(
                                text = "$distanceLabel · ${formatTimestamp(post.timestamp)}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Text(
                            text = formatTimestamp(post.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (onEdit != null || onDelete != null) {
                    PostOverflowMenu(onEdit = onEdit, onDelete = onDelete)
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = post.message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (post.tags.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    post.tags.forEach { tag ->
                        EchoTagChip(tag = tag, onClick = onTagClick?.let { { it(tag) } })
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Like + comment footer
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable {
                            onLikeClick()
                            scope.launch {
                                likeScale.animateTo(1.3f, animationSpec = tween(120))
                                likeScale.animateTo(
                                    1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        }
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isLiked) "Unlike" else "Like",
                        tint = if (isLiked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(20.dp)
                            .scale(likeScale.value)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = likeCount.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.width(20.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Comment,
                    contentDescription = "Comments",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = commentCount.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Owner-only "⋮" menu offering Edit / Delete for a post. */
@Composable
private fun PostOverflowMenu(onEdit: (() -> Unit)?, onDelete: (() -> Unit)?) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (onEdit != null) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = { expanded = false; onEdit() }
                )
            }
            if (onDelete != null) {
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = { expanded = false; onDelete() }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewPostCard() {
    EchoTheme {
        PostCard(
            post = Post(
                id = "1",
                authorId = "preview_uid",
                username = "Sarah Jenkins",
                message = "Just tried the new Blue Bottle on Front St! Best latte in the neighborhood.",
                timestamp = System.currentTimeMillis() - 3 * 60 * 60 * 1000,
                latitude = null,
                longitude = null,
                tags = listOf("coffee", "newopening"),
                likeCount = 24,
                commentCount = 5,
                likedByCurrentUser = true
            ),
            isLiked = true,
            likeCount = 24,
            commentCount = 5,
            onLikeClick = {},
            onClick = {}
        )
    }
}
