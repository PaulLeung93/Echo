package com.example.echo.ui.post

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.echo.components.PostCard
import com.example.echo.models.Comment
import com.example.echo.navigation.Destinations
import com.example.echo.ui.common.BottomNavigationBar
import com.example.echo.utils.formatTimestamp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    navController: NavHostController,
    viewModel: PostDetailViewModel = viewModel()
) {
    // --- UI State ---
    val post by viewModel.post.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLiked by viewModel.isLikedByUser.collectAsState()
    val likeCount by viewModel.likeCount.collectAsState()
    val commentCount by viewModel.commentCount.collectAsState()
    val listState = rememberLazyListState()
    var commentJustAdded by remember { mutableStateOf(false) }



    var newComment by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("feed") }

    val commentTimestamps = remember { mutableStateListOf<Long>() }
    val MAX_COMMENTS = 5
    val WINDOW_MS = 30_000L // 30 seconds

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(postId) {
        viewModel.loadPostAndComments(postId)
    }

    //Scroll up when a new comment is added
    LaunchedEffect(comments.size, commentJustAdded) {
        if (commentJustAdded && comments.isNotEmpty()) {
            listState.animateScrollToItem(comments.size)
            commentJustAdded = false // reset
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Post Details") })
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
                    "map" -> navController.navigate("map") {
                        launchSingleTop = true
                        restoreState = true
                    }
                    "profile" -> { /* No-op */ }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    item {
                        // --- Reused PostCard composable (consistent with Feed & Map) ---
                        PostCard(
                            post = post!!,
                            isLiked = isLiked,
                            likeCount = likeCount,
                            commentCount = commentCount,
                            onLikeClick = { viewModel.toggleLike(postId) },
                            onClick = {},
                            onTagClick = {}, // Tag filtering not needed here
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

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
                        items(comments, key = { it.id ?: it.hashCode() }) { comment ->

                        CommentCard(comment)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // --- Add Comment Input ---
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    OutlinedTextField(
                        value = newComment,
                        onValueChange = { newComment = it },
                        placeholder = { Text("Write a comment.") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val now = System.currentTimeMillis()
                            val recent = commentTimestamps.filter { now - it < WINDOW_MS }

                            if (recent.size >= MAX_COMMENTS) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("You're commenting too fast. Please wait a bit.")
                                }
                            } else {
                                commentTimestamps.add(now)
                                if (newComment.isNotBlank()) {
                                    viewModel.addComment(postId, newComment) {
                                        newComment = ""
                                        commentJustAdded = true //triggers down-scroll to display the comment
                                    }
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
        Column(modifier = Modifier.padding(12.dp)) {
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
