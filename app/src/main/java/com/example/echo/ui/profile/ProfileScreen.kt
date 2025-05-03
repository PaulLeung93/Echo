package com.example.echo.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.echo.components.PostCard
import com.example.echo.models.Post
import com.example.echo.navigation.Destinations
import com.example.echo.ui.auth.AuthViewModel
import com.example.echo.ui.common.BottomNavigationBar
import com.google.firebase.auth.FirebaseAuth

/**
 * ProfileScreen displays the current user's information and their posts.
 * Anonymous users are blocked from accessing this screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    viewModel: ProfileViewModel = viewModel(),
    onPostClick: (Post) -> Unit = {}
) {
    val posts by viewModel.userPosts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: "Anonymous"
    val isAnonymous = FirebaseAuth.getInstance().currentUser?.isAnonymous == true
    var selectedTab by remember { mutableStateOf("profile") }

    if (isAnonymous) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Please sign in to access your profile.", style = MaterialTheme.typography.titleMedium)
        }
        return
    }

    // Scaffold provides top bar and bottom nav layout structure
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("My Profile") })
        },
        bottomBar = {
            BottomNavigationBar(selectedTab = selectedTab) { tab ->
                selectedTab = tab
                when (tab) {
                    "feed" -> {
                        navController.navigate(Destinations.FEED) {
                            popUpTo(Destinations.FEED) { inclusive = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    "map" -> {
                        navController.navigate(Destinations.MAP) {
                            popUpTo(Destinations.FEED) { inclusive = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    "profile" -> {
                        navController.navigate(Destinations.PROFILE) {
                            popUpTo(Destinations.FEED) { inclusive = true }
                            launchSingleTop = true
                            restoreState = true
                        }
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
            // Display user email at the top
            Text("Logged in as: $userEmail", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            when {
                isLoading -> {
                    // Show loading spinner while data is loading
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                posts.isEmpty() -> {
                    // Inform user when they have no posts
                    Text("You haven’t posted anything yet.", style = MaterialTheme.typography.bodyMedium)
                }
                else -> {
                    // Display a scrollable list of the user's posts
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(posts) { post ->
                            PostCard(
                                post = post,
                                isLiked = false, // Could add like state if needed
                                likeCount = viewModel.getLikeCountForPost(post.id),
                                commentCount = viewModel.getCommentCountForPost(post.id),
                                onLikeClick = {},
                                onClick = { onPostClick(post) },
                                onTagClick = {},
                            )
                        }
                    }
                }
            }
        }
    }
}
