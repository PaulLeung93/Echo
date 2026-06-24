package dev.echoapp.echo.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.echoapp.echo.components.BlockUserDialog
import dev.echoapp.echo.components.EmptyState
import dev.echoapp.echo.components.FollowStat
import dev.echoapp.echo.components.PostCard
import dev.echoapp.echo.components.PostCardSkeleton
import dev.echoapp.echo.components.ProfileAvatar
import dev.echoapp.echo.components.ReportDialog
import dev.echoapp.echo.domain.model.Post
import dev.echoapp.echo.utils.Constants

/**
 * Public, read-only view of another user's profile: their avatar, name, @handle,
 * bio, post/like/comment stats, and their posts. Reached by tapping an author
 * anywhere in the app. Owner-only actions (edit/delete) are absent here; viewers
 * can report/block from each post card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    navController: NavHostController,
    viewModel: UserProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var reportTarget by remember { mutableStateOf<Post?>(null) }
    var blockTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    // Profile-level (whole-user) moderation, from the top-bar overflow menu.
    var reportingUser by remember { mutableStateOf(false) }
    var blockingUser by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    val profile = uiState.userProfile
    val displayName = profile?.fullName?.takeIf { it.isNotBlank() }
        ?: profile?.username?.let { "@$it" }
        ?: "Profile"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(displayName, color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    // Whole-user moderation, available on others' profiles only.
                    if (profile != null && !uiState.isSelf && !viewModel.isGuest) {
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    Icons.Filled.MoreVert,
                                    contentDescription = "More options",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Report user") },
                                    onClick = { menuExpanded = false; reportingUser = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("Block user") },
                                    onClick = { menuExpanded = false; blockingUser = true }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when {
            uiState.isLoading && profile == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                EmptyState(
                    icon = Icons.Outlined.CloudOff,
                    title = "Couldn't load this profile",
                    subtitle = uiState.error,
                    isError = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            profile == null -> {
                EmptyState(
                    icon = Icons.Outlined.PersonOff,
                    title = "Profile not found",
                    subtitle = "This user may have deleted their account.",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            else -> {
                val canModerate = !uiState.isSelf && !viewModel.isGuest
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shadowElevation = 4.dp,
                                modifier = Modifier.size(96.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    ProfileAvatar(
                                        photoUrl = profile.photoUrl,
                                        name = profile.fullName.ifBlank { profile.username },
                                        size = 84.dp
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = profile.fullName.ifBlank { profile.username },
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "@${profile.username}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FollowStat(
                                    count = uiState.followerCount,
                                    label = "Followers",
                                    onClick = { navController.navigate("${Constants.ROUTE_FOLLOW_LIST}/${profile.uid}/followers") }
                                )
                                FollowStat(
                                    count = uiState.followingCount,
                                    label = "Following",
                                    onClick = { navController.navigate("${Constants.ROUTE_FOLLOW_LIST}/${profile.uid}/following") }
                                )
                            }
                            if (!uiState.isSelf && !viewModel.isGuest) {
                                Spacer(Modifier.height(12.dp))
                                if (uiState.isFollowing) {
                                    OutlinedButton(
                                        onClick = { viewModel.toggleFollow() },
                                        enabled = !uiState.followInFlight
                                    ) {
                                        Text("Following")
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.toggleFollow() },
                                        enabled = !uiState.followInFlight
                                    ) {
                                        Text("Follow")
                                    }
                                }
                            }
                            profile.bio.takeIf { it.isNotBlank() }?.let { bio ->
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = bio,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
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
                            text = "Posts",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    when {
                        uiState.isLoading && uiState.userPosts.isEmpty() -> items(3) {
                            PostCardSkeleton()
                        }
                        uiState.userPosts.isEmpty() -> item {
                            EmptyState(
                                icon = Icons.Outlined.Article,
                                title = "No posts yet",
                                subtitle = "Echoes they share will show up here.",
                                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                            )
                        }
                        else -> items(uiState.userPosts, key = { it.id }) { post ->
                            PostCard(
                                post = post,
                                isLiked = post.likedByCurrentUser,
                                likeCount = post.likeCount,
                                commentCount = post.commentCount,
                                onLikeClick = { viewModel.toggleLike(post.id) },
                                onClick = { navController.navigate("${Constants.ROUTE_POST_DETAILS}/${post.id}") },
                                onReport = if (canModerate && post.authorId.isNotBlank()) {
                                    { reportTarget = post }
                                } else null,
                                onBlock = if (canModerate && post.authorId.isNotBlank()) {
                                    { blockTarget = post.authorId to post.username }
                                } else null
                            )
                        }
                    }
                }
            }
        }
    }

    reportTarget?.let { post ->
        ReportDialog(
            onDismiss = { reportTarget = null },
            onSubmit = { reason ->
                viewModel.reportPost(post, reason)
                reportTarget = null
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

    // Profile-level report / block (from the top-bar overflow).
    if (reportingUser) {
        ReportDialog(
            onDismiss = { reportingUser = false },
            onSubmit = { reason ->
                viewModel.reportUser(reason)
                reportingUser = false
            }
        )
    }

    if (blockingUser) {
        profile?.let { p ->
            BlockUserDialog(
                username = p.username,
                onDismiss = { blockingUser = false },
                onConfirm = {
                    viewModel.blockUser(p.uid)
                    blockingUser = false
                }
            )
        }
    }
}

@Composable
private fun StatTile(label: String, value: Int, accent: Color, modifier: Modifier = Modifier) {
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
