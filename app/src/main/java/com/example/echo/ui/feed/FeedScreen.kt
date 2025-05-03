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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.echo.navigation.Destinations
import com.example.echo.ui.auth.AuthViewModel
import com.example.echo.utils.Constants
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.example.echo.components.PostCard
import com.example.echo.ui.common.BottomNavigationBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    feedViewModel: FeedViewModel = viewModel()
) {
    val uiState by feedViewModel.uiState.collectAsState()

    var selectedTab by remember { mutableStateOf("feed") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var tagInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val state = uiState
                    if (state is FeedUiState.Success && state.currentTagFilter != null) {
                        // Show filtered tag in top bar with option to clear
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Filtered: #${state.currentTagFilter}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = {
                                feedViewModel.clearTagFilter()
                                tagInput = ""
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Filter")
                            }
                        }
                    } else {
                        Text("Echo", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter by Tag")
                    }
                    IconButton(onClick = {
                        authViewModel.signOut()
                        navController.navigate(Destinations.SIGN_IN) {
                            popUpTo(Destinations.FEED) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Destinations.CREATE_POST) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Post", tint = Color.White)
            }
        },
        bottomBar = {
            BottomNavigationBar(selectedTab = selectedTab) { tab ->
                selectedTab = tab
                if (tab == "map") {
                    navController.navigate(Destinations.MAP) {
                        popUpTo(Destinations.FEED) { inclusive = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is FeedUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is FeedUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            is FeedUiState.Success -> {
                SwipeRefresh(
                    state = rememberSwipeRefreshState(state.isRefreshing),
                    onRefresh = { feedViewModel.refreshPosts() }
                ) {
                    LazyColumn(
                        contentPadding = paddingValues,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        if (state.filteredPosts.isEmpty()) {
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
                            items(state.filteredPosts) { post ->
                                val isLiked = state.userLikes.contains(post.id)
                                val likeCount = state.postLikes[post.id] ?: 0
                                val commentCount = state.commentLikes[post.id] ?: 0

                                PostCard(
                                    post = post,
                                    isLiked = isLiked,
                                    likeCount = likeCount,
                                    commentCount = commentCount,
                                    onLikeClick = { feedViewModel.toggleLike(post.id) },
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
}
