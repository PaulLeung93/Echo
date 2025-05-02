package com.example.echo.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.echo.models.Post
import com.example.echo.utils.formatTimestamp

@Composable
fun PostCard(
    post: Post,
    isLiked: Boolean,
    likeCount: Int,
    commentCount: Int,
    onLikeClick: () -> Unit,
    onClick: () -> Unit,
    onTagClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier) {

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(6.dp)
    ) {

        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = post.username,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = post.message,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = formatTimestamp(post.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                // Spacer to push everything else to the right
                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = onLikeClick) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = if (isLiked) "Unlike" else "Like",
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }

                Text("$likeCount ${if (likeCount == 1) "like" else "likes"}")

                Spacer(Modifier.width(8.dp))

                Icon(Icons.Default.Message, contentDescription = "Comments")

                Spacer(Modifier.width(4.dp))

                Text("$commentCount ${if (commentCount == 1) "comment" else "comments"}")
            }

            if (post.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    post.tags.forEach { tag ->
                        AssistChip(
                            onClick = {
                                onTagClick?.invoke(tag)
                            },
                            label = { Text(text = "$tag") }
                        )
                    }
                }
            }

        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPostCard() {
    PostCard(
        post = Post(
            username = "preview_user",
            message = "This is a preview of a post in Echo.",
            timestamp = System.currentTimeMillis(),
            tags = listOf("cs101", "finals")
        ),
        isLiked = true,
        likeCount = 5,
        commentCount = 2,
        onLikeClick = {},
        onClick = {}
    )
}

