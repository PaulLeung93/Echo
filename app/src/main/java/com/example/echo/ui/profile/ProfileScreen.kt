package com.example.echo.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.echo.R
import com.example.echo.components.PostCard
import com.example.echo.domain.model.Post
import com.example.echo.navigation.Destinations
import com.example.echo.ui.auth.AuthViewModel

/**
 * ProfileScreen displays the current user's info, total stats, and their posts.
 * Anonymous users are blocked from accessing this screen.
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

    // Redirect anonymous users to sign-in screen
    LaunchedEffect(currentUser) {
        if (currentUser == null || isAnonymous) {
            navController.navigate(Destinations.SIGN_IN) {
                popUpTo(Destinations.PROFILE) { inclusive = true }
            }
        }
    }

    if (currentUser == null || isAnonymous) return

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    "My Profile",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Profile Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_profile_placeholder),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(text = userEmail, style = MaterialTheme.typography.titleMedium)

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatColumn(label = "Posts", value = uiState.userPosts.size)
                    StatColumn(label = "Likes", value = uiState.totalLikes)
                    StatColumn(label = "Comments", value = uiState.totalComments)
                }
            }

            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isLoading && uiState.userPosts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(uiState.error ?: "Error loading posts", color = MaterialTheme.colorScheme.error)
                }
            } else if (uiState.userPosts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("You haven’t posted anything yet.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(uiState.userPosts) { post ->
                        PostCard(
                            post = post,
                            isLiked = post.likedByCurrentUser,
                            likeCount = post.likeCount,
                            commentCount = post.commentCount,
                            onLikeClick = { viewModel.toggleLike(post.id) },
                            onClick = {
                                navController.navigate("${com.example.echo.utils.Constants.ROUTE_POST_DETAILS}/${post.id}")
                            },
                            onTagClick = {},
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value", style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
