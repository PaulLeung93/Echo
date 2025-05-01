package com.example.echo.ui.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    val posts by feedViewModel.posts.collectAsState()
    val isRefreshing by feedViewModel.isRefreshing.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf("feed") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Echo",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = {
                        // Sign out and return to SignInScreen
                        authViewModel.signOut()
                        navController.navigate(Destinations.SIGN_IN) {
                            popUpTo(Destinations.FEED) { inclusive = true }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Sign Out"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Destinations.CREATE_POST) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Post",
                    tint = Color.White
                )
            }
        },
        bottomBar = {
            BottomNavigationBar(selectedTab = selectedTab) { tab ->
                selectedTab = tab
                when (tab) {
                    "map" -> navController.navigate(Destinations.MAP) {
                        popUpTo(Destinations.FEED) { inclusive = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                    "profile" -> {
                        // Placeholder for ProfileScreen
                    }
                }
            }
        }
    ) { paddingValues ->
        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing),
            onRefresh = { feedViewModel.refreshPosts() }
        ) {
            LazyColumn(
                contentPadding = paddingValues,
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
                        val postLikes by feedViewModel.postLikes.collectAsState()
                        val userLikes by feedViewModel.userLikes.collectAsState()
                        val commentLikes by feedViewModel.commentLikes.collectAsState()

                        val isLiked = userLikes.contains(post.id)
                        val likeCount = postLikes[post.id] ?: 0
                        val commentCount = commentLikes[post.id] ?: 0

                        PostCard(
                            post = post,
                            isLiked = isLiked,
                            likeCount = likeCount,
                            commentCount = commentCount,
                            onLikeClick = { post.id.let { feedViewModel.toggleLike(it) } },
                            onClick = {
                                post.id.let { postId ->
                                    navController.navigate("${Constants.ROUTE_POST_DETAILS}/$postId")
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
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
            PostCard(post, isLiked = false, likeCount = 0, commentCount = 2, onLikeClick = {}, onClick = {})
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
