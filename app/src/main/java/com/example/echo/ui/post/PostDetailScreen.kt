package com.example.echo.ui.post

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.echo.models.Comment
import com.example.echo.ui.common.BottomNavigationBar
import com.example.echo.utils.formatTimestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    navController: NavHostController,
    viewModel: PostDetailViewModel = viewModel()
) {
    val post by viewModel.post.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var newComment by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("feed") }

    LaunchedEffect(postId) {
        viewModel.loadPostAndComments(postId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post Details") }
            )
        },
        bottomBar = {
            BottomNavigationBar(selectedTab = selectedTab) { tab ->
                selectedTab = tab
                when (tab) {
                    "feed" -> navController.navigate("feed") {
                        popUpTo("feed") { inclusive = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                    "map" -> {
                        navController.navigate("map") {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    "profile" -> {
                        // Profile placeholder
                    }
                }
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = errorMessage ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (post != null) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    item {
                        // Post Details
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = post!!.username,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = post!!.message,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = formatTimestamp(post!!.timestamp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Comments",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (comments.isEmpty()) {
                        item {
                            Text(
                                text = "No comments yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        items(comments) { comment ->
                            CommentCard(comment)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    OutlinedTextField(
                        value = newComment,
                        onValueChange = { newComment = it },
                        placeholder = { Text("Write a comment...") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newComment.isNotBlank()) {
                                viewModel.addComment(postId, newComment) {
                                    newComment = ""
                                }
                            }
                        },
                        enabled = newComment.isNotBlank()
                    ) {
                        Text("Send")
                    }
                }
            }
        }
    }
}

@Composable
fun CommentCard(comment: Comment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = comment.username,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = comment.message,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatTimestamp(comment.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
