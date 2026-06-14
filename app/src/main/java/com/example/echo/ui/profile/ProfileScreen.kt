package com.example.echo.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.echo.components.AuthorAvatar
import com.example.echo.components.PostCard
import com.example.echo.domain.model.Post
import com.example.echo.navigation.Destinations
import com.example.echo.ui.auth.AuthViewModel
import com.example.echo.utils.Constants

/**
 * ProfileScreen shows the current user's info, total stats, and their posts —
 * each with an owner-only edit/delete menu. Anonymous users are redirected out.
 */
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()

    val currentUser = authState.currentUser
    val userEmail = currentUser?.email ?: "Anonymous"
    val isAnonymous = currentUser?.isAnonymous == true

    LaunchedEffect(currentUser) {
        if (currentUser == null || isAnonymous) {
            navController.navigate(Destinations.SIGN_IN) {
                popUpTo(Destinations.PROFILE) { inclusive = true }
            }
        }
    }
    if (currentUser == null || isAnonymous) return

    var postToEdit by remember { mutableStateOf<Post?>(null) }
    var postToDelete by remember { mutableStateOf<Post?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    val displayName = userEmail.substringBefore("@")

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("My Profile", color = MaterialTheme.colorScheme.onPrimary) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar with a soft cream ring + shadow (wireframe style).
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(96.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AuthorAvatar(name = displayName, size = 84.dp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "@$displayName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StatTile("Posts", uiState.userPosts.size, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                        StatTile("Likes", uiState.totalLikes, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                        StatTile("Comments", uiState.totalComments, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            item {
                Text(
                    text = "Your posts",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            when {
                uiState.isLoading && uiState.userPosts.isEmpty() -> item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null -> item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(uiState.error ?: "Error loading posts", color = MaterialTheme.colorScheme.error)
                    }
                }
                uiState.userPosts.isEmpty() -> item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "You haven't shared an echo yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> items(uiState.userPosts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        isLiked = post.likedByCurrentUser,
                        likeCount = post.likeCount,
                        commentCount = post.commentCount,
                        onLikeClick = { viewModel.toggleLike(post.id) },
                        onClick = { navController.navigate("${Constants.ROUTE_POST_DETAILS}/${post.id}") },
                        onEdit = { postToEdit = post },
                        onDelete = { postToDelete = post }
                    )
                }
            }
        }
    }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Edit dialog
    postToEdit?.let { post ->
        var editText by remember(post.id) { mutableStateOf(post.message) }
        AlertDialog(
            onDismissRequest = { postToEdit = null },
            title = { Text("Edit echo") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    minLines = 3
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editText.isNotBlank()) viewModel.updatePost(post.id, editText.trim())
                        postToEdit = null
                    },
                    enabled = editText.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { postToEdit = null }) { Text("Cancel") } }
        )
    }

    // Delete confirmation
    postToDelete?.let { post ->
        AlertDialog(
            onDismissRequest = { postToDelete = null },
            title = { Text("Delete echo?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePost(post.id)
                        postToDelete = null
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { postToDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun StatTile(label: String, value: Int, accent: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 1.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value.toString(), style = MaterialTheme.typography.headlineSmall, color = accent)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
