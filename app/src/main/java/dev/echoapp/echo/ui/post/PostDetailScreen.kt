package dev.echoapp.echo.ui.post

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import dev.echoapp.echo.components.BlockUserDialog
import dev.echoapp.echo.components.DeletePostDialog
import dev.echoapp.echo.components.EditPostDialog
import dev.echoapp.echo.components.PostCard
import dev.echoapp.echo.components.CommentCard
import dev.echoapp.echo.components.EmptyState
import dev.echoapp.echo.components.ReportDialog
import dev.echoapp.echo.navigation.Destinations
import dev.echoapp.echo.domain.model.Comment
import dev.echoapp.echo.domain.model.Post
import dev.echoapp.echo.utils.formatTimestamp
import kotlinx.coroutines.launch

import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    navController: NavHostController,
    viewModel: PostDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var commentJustAdded by remember { mutableStateOf(false) }

    var newComment by remember { mutableStateOf("") }
    val commentTimestamps = remember { mutableStateListOf<Long>() }
    val MAX_COMMENTS = 5
    val WINDOW_MS = 30_000L // 30 seconds

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Moderation dialog state.
    var reportingPost by remember { mutableStateOf(false) }
    var reportingComment by remember { mutableStateOf<Comment?>(null) }
    // The user being blocked (uid to username) — set from a post or a comment.
    var blockTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    // Own-post edit / delete dialog state.
    var editingPost by remember { mutableStateOf<Post?>(null) }
    var deletingPost by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    // Scroll up when a new comment is added
    LaunchedEffect(uiState.comments.size, commentJustAdded) {
        if (commentJustAdded && uiState.comments.isNotEmpty()) {
            listState.animateScrollToItem(uiState.comments.size)
            commentJustAdded = false // reset
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Post", color = MaterialTheme.colorScheme.onPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimary)
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
            if (uiState.isLoading && uiState.post == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                EmptyState(
                    icon = Icons.Outlined.CloudOff,
                    title = "Couldn't load this post",
                    subtitle = uiState.error,
                    isError = true,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (uiState.post != null) {
                val post = uiState.post!!
                val canModeratePost = !viewModel.isGuest && post.authorId.isNotBlank() &&
                    post.authorId != uiState.currentUserId
                val isOwnPost = !viewModel.isGuest && post.authorId.isNotBlank() &&
                    post.authorId == uiState.currentUserId
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    item {
                        PostCard(
                            post = post,
                            isLiked = post.likedByCurrentUser,
                            likeCount = post.likeCount,
                            commentCount = post.commentCount,
                            onLikeClick = { viewModel.toggleLike() },
                            onClick = {},
                            onTagClick = {},
                            onReport = if (canModeratePost) {
                                { reportingPost = true }
                            } else null,
                            onBlock = if (canModeratePost) {
                                { blockTarget = post.authorId to post.username }
                            } else null,
                            onEdit = if (isOwnPost) {
                                { editingPost = post }
                            } else null,
                            onDelete = if (isOwnPost) {
                                { deletingPost = true }
                            } else null,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Text(
                            text = "Comments (${uiState.comments.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (uiState.comments.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Outlined.ChatBubbleOutline,
                                title = "No comments yet",
                                subtitle = "Be the first to comment.",
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                            )
                        }
                    } else {
                        items(uiState.comments, key = { it.id ?: it.hashCode() }) { comment ->
                            val isOwnComment = uiState.currentUserId != null &&
                                comment.authorId.isNotEmpty() &&
                                comment.authorId == uiState.currentUserId
                            CommentCard(
                                comment = comment,
                                isAuthor = comment.authorId.isNotEmpty() &&
                                    comment.authorId == post.authorId,
                                onDelete = if (isOwnComment) {
                                    { viewModel.deleteComment(comment.id) }
                                } else {
                                    null
                                },
                                onReport = if (!viewModel.isGuest && !isOwnComment &&
                                    comment.authorId.isNotEmpty()
                                ) {
                                    { reportingComment = comment }
                                } else null,
                                onBlock = if (!viewModel.isGuest && !isOwnComment &&
                                    comment.authorId.isNotEmpty()
                                ) {
                                    { blockTarget = comment.authorId to comment.username }
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
                        placeholder = { Text("Write a comment…") },
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    val canSend = newComment.isNotBlank()
                    Surface(
                        shape = CircleShape,
                        color = if (canSend) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .clickable(enabled = canSend) {
                                val now = System.currentTimeMillis()
                                val recent = commentTimestamps.filter { now - it < WINDOW_MS }
                                if (recent.size >= MAX_COMMENTS) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("You're commenting too fast. Please wait a bit.")
                                    }
                                } else {
                                    commentTimestamps.add(now)
                                    viewModel.addComment(newComment) {
                                        newComment = ""
                                        commentJustAdded = true
                                    }
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send comment",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                if (reportingPost) {
                    ReportDialog(
                        onDismiss = { reportingPost = false },
                        onSubmit = { reason ->
                            viewModel.reportPost(reason)
                            reportingPost = false
                        }
                    )
                }

                reportingComment?.let { comment ->
                    ReportDialog(
                        onDismiss = { reportingComment = null },
                        onSubmit = { reason ->
                            viewModel.reportComment(comment, reason)
                            reportingComment = null
                        }
                    )
                }

                blockTarget?.let { (uid, username) ->
                    BlockUserDialog(
                        username = username,
                        onDismiss = { blockTarget = null },
                        onConfirm = {
                            viewModel.blockUser(uid)
                            blockTarget = null
                        }
                    )
                }

                editingPost?.let { editPost ->
                    EditPostDialog(
                        initialText = editPost.message,
                        onConfirm = { newMessage -> viewModel.updatePost(newMessage) },
                        onDismiss = { editingPost = null }
                    )
                }

                if (deletingPost) {
                    DeletePostDialog(
                        onConfirm = { viewModel.deletePost { navController.popBackStack() } },
                        onDismiss = { deletingPost = false }
                    )
                }
            }
        }
    }
}

