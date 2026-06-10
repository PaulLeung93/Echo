package com.example.echo.feature.map.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.echo.components.CommentCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoiDetailScreen(
    navController: NavHostController,
    viewModel: PoiDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var commentJustAdded by remember { mutableStateOf(false) }

    var newComment by remember { mutableStateOf("") }
    val commentTimestamps = remember { mutableStateListOf<Long>() }
    val MAX_COMMENTS = 5
    val WINDOW_MS = 30_000L // 30 seconds

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Scroll up when a new comment is added
    LaunchedEffect(uiState.comments.size, commentJustAdded) {
        if (commentJustAdded && uiState.comments.isNotEmpty()) {
            listState.animateScrollToItem(uiState.comments.size) // Scroll to bottom (comments are appending?)
            // Actually usually comments list might be inverted or we append to bottom.
            // PostDetailScreen scrolled to 'uiState.comments.size'.
            // If we have a header item, we need to account for it.
            // PostDetailScreen had 1 header item (PostCard) plus comments.
            // usage: listState.animateScrollToItem(uiState.comments.size) -> index = size.
            // if we have 1 header, indices are 0 (header), 1..size (comments).
            // So if size is 3, indices are 0, 1, 2, 3. Item 3 is the LAST one.
            // So scrolling to size accounts for 1 header item.
            // Here we will use a LazyColumn.
            commentJustAdded = false // reset
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("POI Details", color = MaterialTheme.colorScheme.onPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            if (uiState.isLoading && uiState.poi == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = uiState.error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (uiState.poi != null) {
                val poi = uiState.poi!!
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // Header Item
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = poi.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                SuggestionChip(
                                    onClick = { },
                                    label = { Text(poi.type.replaceFirstChar { it.uppercase() }) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = poi.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "Comments",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (uiState.comments.isEmpty()) {
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
                        items(uiState.comments, key = { it.id }) { comment ->
                            CommentCard(
                                comment = comment,
                                onDelete = if (comment.username == uiState.currentUserEmail) {
                                    { viewModel.deleteComment(comment.id) }
                                } else null
                            )
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
                        .padding(bottom = 16.dp, top = 8.dp)
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
                            // Simple rate limiting
                            val recent = commentTimestamps.filter { now - it < WINDOW_MS }
                            if (recent.size >= MAX_COMMENTS) {
                                // In a real app we'd show a snackbar
                            } else {
                                commentTimestamps.add(now)
                                if (newComment.isNotBlank()) {
                                    viewModel.addComment(newComment) {
                                        newComment = ""
                                        commentJustAdded = true
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
