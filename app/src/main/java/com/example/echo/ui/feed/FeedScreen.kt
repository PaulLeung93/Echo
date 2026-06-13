package com.example.echo.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.echo.domain.model.Coordinates
import com.example.echo.navigation.Destinations
import com.example.echo.ui.auth.AuthViewModel
import com.example.echo.utils.Constants
import com.example.echo.utils.distanceMeters
import com.example.echo.utils.formatDistance
import com.example.echo.components.PostCard
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    feedViewModel: FeedViewModel = hiltViewModel()
) {
    val uiState by feedViewModel.uiState.collectAsState()
    val isRefreshing by feedViewModel.isRefreshing.collectAsState()
    val userCoords by feedViewModel.userCoordinates.collectAsState()
    
    val authState by authViewModel.uiState.collectAsState()
    val isUserAuthenticated = authState.currentUser != null
    val isGuest = authState.currentUser?.isAnonymous == true

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showFilterDialog by remember { mutableStateOf(false) }
    var tagInput by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- Top App Bar ---
            TopAppBar(
                title = {
                    if (uiState.currentTag != null) {
                        // Show filtered tag in top bar with option to clear
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Filtered: #${uiState.currentTag}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            IconButton(
                                onClick = {
                                    feedViewModel.clearTagFilter()
                                    tagInput = ""
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Filter")
                            }
                        }
                    } else {
                        Text(
                            "Echo",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showFilterDialog = true },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter by Tag")
                    }
                    IconButton(
                        onClick = {
                            authViewModel.signOut()
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )

            // --- Main Feed Content ---
            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.isLoading && uiState.posts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.error != null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    SwipeRefresh(
                        state = rememberSwipeRefreshState(isRefreshing),
                        onRefresh = { feedViewModel.refreshPosts() }
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (uiState.posts.isEmpty()) {
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
                                items(uiState.posts) { post ->
                                    val distanceLabel = remember(post.id, userCoords) {
                                        val u = userCoords
                                        val lat = post.latitude
                                        val lng = post.longitude
                                        if (u != null && lat != null && lng != null) {
                                            formatDistance(distanceMeters(u, Coordinates(lat, lng)))
                                        } else null
                                    }
                                    PostCard(
                                        post = post,
                                        isLiked = post.likedByCurrentUser,
                                        likeCount = post.likeCount,
                                        commentCount = post.commentCount,
                                        distanceLabel = distanceLabel,
                                        onLikeClick = {
                                            if (isGuest) {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Sign in to like posts")
                                                }
                                            } else {
                                                feedViewModel.toggleLike(post.id)
                                            }
                                        },
                                        onClick = {
                                            navController.navigate("${Constants.ROUTE_POST_DETAILS}/${post.id}")
                                        },
                                        onTagClick = { tag ->
                                            tagInput = tag
                                            feedViewModel.setTagFilter(tag)
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // --- Filter Dialog ---
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filter Posts by Tag") },
            text = {
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    label = { Text("Enter tag") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    feedViewModel.setTagFilter(tagInput.trim().lowercase())
                    showFilterDialog = false
                }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    feedViewModel.clearTagFilter()
                    tagInput = ""
                    showFilterDialog = false
                }) {
                    Text("Clear")
                }
            }
        )
    }
}
