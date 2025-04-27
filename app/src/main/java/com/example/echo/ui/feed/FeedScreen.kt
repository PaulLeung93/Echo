package com.example.echo.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.echo.models.Post
import com.example.echo.navigation.Destinations
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FeedScreen(
    feedViewModel: FeedViewModel = viewModel(),
    navController: NavHostController
) {
    val posts by feedViewModel.posts.collectAsState()
    val isRefreshing = remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate(Destinations.CREATE_POST)
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Post",
                    tint = Color.White
                )
            }
        }
    ) { padding ->
        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing = isRefreshing.value),
            onRefresh = {
                isRefreshing.value = true
                feedViewModel.refreshPosts {
                    isRefreshing.value = false
                }
            }
        ) {
            LazyColumn(
                contentPadding = padding,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (posts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No posts yet.\nBe the first to share something!",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    items(posts) { post ->
                        PostCard(post)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}


@Composable
fun PostCard(post: Post) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = post.username, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = post.message, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatTimestamp(post.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

// Helper to format timestamp
fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hr ago"
        else -> "$days day${if (days > 1) "s" else ""} ago"
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewPostCard() {
    PostCard(
        post = Post(
            username = "preview_user",
            message = "This is a preview of a post in Echo.",
            timestamp = System.currentTimeMillis()
        )
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewFeedScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(
            listOf(
                Post(username = "paul_dev", message = "Excited to launch Echo!", timestamp = System.currentTimeMillis()),
                Post(username = "jane_doe", message = "Loving the local vibes!", timestamp = System.currentTimeMillis() - 60000),
                Post(username = "john_doe", message = "Anyone else got cooked from that exam??", timestamp = System.currentTimeMillis() - 3600000)
            )
        ) { post ->
            PostCard(post)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
