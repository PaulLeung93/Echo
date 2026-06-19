package com.example.echo.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.echo.R
import com.example.echo.domain.model.Coordinates
import com.example.echo.navigation.Destinations
import com.example.echo.ui.auth.AuthViewModel
import com.example.echo.utils.Constants
import com.example.echo.utils.distanceMeters
import com.example.echo.utils.formatDistance
import com.example.echo.components.BlockUserDialog
import com.example.echo.components.EmptyState
import com.example.echo.components.PostCard
import com.example.echo.components.PostCardSkeleton
import com.example.echo.components.ReportDialog
import com.example.echo.domain.model.Post
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
    val isLoadingMore by feedViewModel.isLoadingMore.collectAsState()
    val userCoords by feedViewModel.userCoordinates.collectAsState()
    val neighborhoodName by feedViewModel.neighborhoodName.collectAsState()

    // Drive feed pagination: ask for the next page once the user scrolls within a few
    // items of the end. Only the untagged feed pages (the VM ignores it otherwise).
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) feedViewModel.loadMore()
    }
    
    val authState by authViewModel.uiState.collectAsState()
    val isUserAuthenticated = authState.currentUser != null
    val isGuest = authState.currentUser?.isAnonymous == true

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showFilterDialog by remember { mutableStateOf(false) }
    var tagInput by remember { mutableStateOf("") }
    // Posts pending a report / block confirmation (others' posts only).
    var reportTarget by remember { mutableStateOf<Post?>(null) }
    var blockTarget by remember { mutableStateOf<Post?>(null) }

    LaunchedEffect(Unit) {
        feedViewModel.uiEvent.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- Top App Bar ---
            CenterAlignedTopAppBar(
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
                        if (!neighborhoodName.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(percent = 50),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = stringResource(R.string.location_chip_desc),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = neighborhoodName!!,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (uiState.currentTag == null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 12.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.echo_logo),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Echo",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )

            // --- Main Feed Content ---
            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.isLoading && uiState.posts.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        repeat(5) { PostCardSkeleton() }
                    }
                } else if (uiState.error != null) {
                    EmptyState(
                        icon = Icons.Outlined.CloudOff,
                        title = "Couldn't load the feed",
                        subtitle = uiState.error,
                        isError = true,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    SwipeRefresh(
                        state = rememberSwipeRefreshState(isRefreshing),
                        onRefresh = { feedViewModel.refreshPosts() }
                    ) {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (uiState.posts.isEmpty()) {
                                item {
                                    EmptyState(
                                        icon = Icons.Outlined.Forum,
                                        title = "No echoes yet",
                                        subtitle = "Be the first to share something with your neighborhood.",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 64.dp)
                                    )
                                }
                            } else {
                                items(uiState.posts, key = { it.id }) { post ->
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
                                        },
                                        onReport = if (!isGuest && post.authorId.isNotBlank() &&
                                            post.authorId != feedViewModel.currentUserId
                                        ) {
                                            { reportTarget = post }
                                        } else null,
                                        onBlock = if (!isGuest && post.authorId.isNotBlank() &&
                                            post.authorId != feedViewModel.currentUserId
                                        ) {
                                            { blockTarget = post }
                                        } else null
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                                if (isLoadingMore) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                        }
                                    }
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

    reportTarget?.let { post ->
        ReportDialog(
            onDismiss = { reportTarget = null },
            onSubmit = { reason ->
                feedViewModel.reportPost(post, reason)
                reportTarget = null
            }
        )
    }

    blockTarget?.let { post ->
        BlockUserDialog(
            username = post.username,
            onDismiss = { blockTarget = null },
            onConfirm = {
                feedViewModel.blockUser(post)
                blockTarget = null
            }
        )
    }
}
