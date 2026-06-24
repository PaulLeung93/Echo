package dev.echoapp.echo.ui.poi

import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.echoapp.echo.R
import dev.echoapp.echo.components.BlockUserDialog
import dev.echoapp.echo.components.DeletePostDialog
import dev.echoapp.echo.components.EditPostDialog
import dev.echoapp.echo.components.PostCard
import dev.echoapp.echo.components.ReportDialog
import dev.echoapp.echo.domain.model.Poi
import dev.echoapp.echo.domain.model.Post
import dev.echoapp.echo.utils.Constants
import dev.echoapp.echo.utils.formatDistance

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PoiDetailScreen(
    navController: NavHostController,
    viewModel: PoiDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val density = LocalDensity.current

    var reportTarget by remember { mutableStateOf<Post?>(null) }
    var blockTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var postToEdit by remember { mutableStateOf<Post?>(null) }
    var postToDelete by remember { mutableStateOf<Post?>(null) }

    val proximityKm = (Constants.PROXIMITY_RADIUS_METERS / 1000).toInt()

    // Drives the collapsing hero: the image (item 0) parallaxes/fades as it scrolls
    // away, and once it's mostly gone the POI name fades into the top bar.
    val heroHeightPx = with(density) { 220.dp.toPx() }
    val collapsed by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
                listState.firstVisibleItemScrollOffset > heroHeightPx * 0.7f
        }
    }
    val titleBarAlpha by animateFloatAsState(
        targetValue = if (collapsed) 1f else 0f,
        label = "poiTitleBarAlpha"
    )

    // Surface transient action errors (failed like/report) as snackbars.
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PlaceDetailTopBar(
                onBack = { navController.popBackStack() },
                title = uiState.poi?.name,
                titleAlpha = titleBarAlpha,
                // The star shows for signed-in users once the POI has loaded; the
                // ViewModel handles range/slot/hold eligibility and the messaging.
                showFavorite = uiState.poi != null && !uiState.isGuest,
                isFavorited = uiState.isFavorited,
                onToggleFavorite = viewModel::toggleFavorite,
                // Enabled only once the POI has loaded.
                onShare = uiState.poi?.let { poi ->
                    {
                        val shareText = context.getString(
                            R.string.share_place_text, poi.name, poi.description
                        )
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, poi.name)
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(
                            Intent.createChooser(
                                sendIntent,
                                context.getString(R.string.share_place)
                            )
                        )
                    }
                }
            )
        },
        bottomBar = {
            uiState.poi?.let { poi ->
                AddPostBar(
                    canPost = uiState.canPost,
                    gateMessage = postGateMessage(uiState, proximityKm),
                    onAddPost = {
                        navController.navigate("${Constants.ROUTE_CREATE_POST}?poiId=${poi.id}")
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.poi == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.error != null -> {
                    Text(
                        text = uiState.error ?: stringResource(R.string.unknown_error),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp)
                    )
                }

                uiState.poi != null -> {
                    val poi = uiState.poi!!
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        // Hero image (item 0): parallaxes up at half-speed while fading and
                        // shrinking as it scrolls off, so it glides past the top of the screen.
                        item(key = "hero") {
                            Column(
                                modifier = Modifier.graphicsLayer {
                                    val offset = if (listState.firstVisibleItemIndex == 0) {
                                        listState.firstVisibleItemScrollOffset.toFloat()
                                    } else 0f
                                    val frac = (offset / heroHeightPx).coerceIn(0f, 1f)
                                    translationY = offset * 0.5f
                                    alpha = 1f - frac
                                    val s = 1f - 0.08f * frac
                                    scaleX = s
                                    scaleY = s
                                }
                            ) {
                                Spacer(Modifier.height(8.dp))
                                HeroImage(poi, modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }

                        // Place details: scroll away normally beneath the hero.
                        item(key = "details") {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                Spacer(Modifier.height(20.dp))
                                TitleRow(poi)
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = poi.description,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(20.dp))
                                ProximityBanner(uiState)
                                Spacer(Modifier.height(20.dp))
                            }
                        }

                        // Echoes count + sort toggle: pins under the top bar so the controls
                        // stay reachable once the hero/details have scrolled away.
                        stickyHeader(key = "echoes") {
                            ThreadHeader(
                                count = uiState.posts.size,
                                sortDescending = uiState.sortDescending,
                                onToggleSort = viewModel::toggleSort,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        if (uiState.posts.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.poi_thread_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 24.dp)
                                )
                            }
                        } else {
                            items(uiState.posts, key = { it.id }) { post ->
                                val isOwnPost = uiState.currentUserId != null &&
                                    post.authorId.isNotEmpty() &&
                                    post.authorId == uiState.currentUserId
                                val canModerate = !uiState.isGuest && !isOwnPost &&
                                    post.authorId.isNotEmpty()
                                PostCard(
                                    post = post,
                                    isLiked = post.likedByCurrentUser,
                                    likeCount = post.likeCount,
                                    commentCount = post.commentCount,
                                    onLikeClick = { viewModel.toggleLike(post.id) },
                                    onClick = {
                                        navController.navigate("${Constants.ROUTE_POST_DETAILS}/${post.id}")
                                    },
                                    onAuthorClick = if (post.authorId.isNotEmpty() &&
                                        post.authorId != uiState.currentUserId
                                    ) {
                                        { navController.navigate("${Constants.ROUTE_USER_PROFILE}/${post.authorId}") }
                                    } else null,
                                    onReport = if (canModerate) {
                                        { reportTarget = post }
                                    } else null,
                                    onBlock = if (canModerate) {
                                        { blockTarget = post.authorId to post.username }
                                    } else null,
                                    onEdit = if (isOwnPost) {
                                        { postToEdit = post }
                                    } else null,
                                    onDelete = if (isOwnPost) {
                                        { postToDelete = post }
                                    } else null,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                            }
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

    postToEdit?.let { post ->
        EditPostDialog(
            initialText = post.message,
            onConfirm = { newMessage -> viewModel.updatePost(post.id, newMessage) },
            onDismiss = { postToEdit = null }
        )
    }

    postToDelete?.let { post ->
        DeletePostDialog(
            onConfirm = { viewModel.deletePost(post.id) },
            onDismiss = { postToDelete = null }
        )
    }
}

@Composable
private fun PlaceDetailTopBar(
    onBack: () -> Unit,
    onShare: (() -> Unit)?,
    title: String? = null,
    titleAlpha: Float = 0f,
    showFavorite: Boolean = false,
    isFavorited: Boolean = false,
    onToggleFavorite: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.back),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            onClick = onBack
        )
        // POI name fades in only once the hero has collapsed; weight(1f) keeps the
        // share button pinned to the end whether or not the title is showing.
        Text(
            text = title.orEmpty(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .graphicsLayer { alpha = titleAlpha }
        )
        if (showFavorite) {
            CircleIconButton(
                icon = if (isFavorited) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = stringResource(
                    if (isFavorited) R.string.poi_unfavorite else R.string.poi_favorite
                ),
                containerColor = if (isFavorited) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLowest
                },
                bordered = !isFavorited,
                onClick = onToggleFavorite
            )
        }
        CircleIconButton(
            icon = Icons.Filled.Share,
            contentDescription = stringResource(R.string.share_place),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            bordered = true,
            enabled = onShare != null,
            onClick = { onShare?.invoke() }
        )
    }
}

@Composable
private fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    containerColor: androidx.compose.ui.graphics.Color,
    bordered: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = containerColor,
        shadowElevation = 1.dp,
        border = if (bordered) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        } else null,
        modifier = Modifier
            .size(40.dp)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun HeroImage(poi: Poi, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        // The type icon is the base layer, so it shows whenever there's no photo
        // to cover it: blank URL, while loading, or on a failed load (e.g. a
        // dead Wikimedia link). A successful photo draws on top and hides it.
        Icon(
            imageVector = typeIcon(poi.type),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.size(72.dp)
        )
        if (!poi.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(poi.imageUrl)
                    // POI photos are hotlinked from Wikimedia, which 403s the
                    // default "okhttp/…" User-Agent; their policy requires a
                    // descriptive one with contact info.
                    .setHeader("User-Agent", Constants.IMAGE_USER_AGENT)
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.poi_photo_desc, poi.name),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun TitleRow(poi: Poi) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = poi.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        TypeChip(poi.type)
    }
}

@Composable
private fun TypeChip(type: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = typeIcon(type),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = type.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun ProximityBanner(uiState: PoiDetailUiState) {
    val (message, inRange) = proximityMessage(uiState)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProximityRipple(active = inRange)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** The "echo" signature motif: a pulsing ring behind a near-me badge. */
@Composable
private fun ProximityRipple(active: Boolean) {
    val tint = if (active) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.outline
    }
    Box(
        modifier = Modifier.size(40.dp),
        contentAlignment = Alignment.Center
    ) {
        if (active) {
            val transition = rememberInfiniteTransition(label = "proximityRipple")
            val scale by transition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(1800, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "scale"
            )
            val alpha by transition.animateFloat(
                initialValue = 0.45f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    tween(1800, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "alpha"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scale)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = alpha))
            )
        }
        Surface(
            shape = CircleShape,
            color = tint,
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.NearMe,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ThreadHeader(
    count: Int,
    sortDescending: Boolean,
    onToggleSort: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Forum,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.echoes_header),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        // Only worth showing the order toggle once there's more than one post.
        if (count > 1) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.clickable(onClick = onToggleSort)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SwapVert,
                        contentDescription = stringResource(R.string.poi_sort_toggle),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(
                            if (sortDescending) R.string.poi_sort_newest else R.string.poi_sort_oldest
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AddPostBar(
    canPost: Boolean,
    gateMessage: String?,
    onAddPost: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp)
        ) {
            if (gateMessage != null) {
                Text(
                    text = gateMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }
            Button(
                onClick = onAddPost,
                enabled = canPost,
                shape = RoundedCornerShape(percent = 50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.poi_add_post),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** Returns the reason posting is unavailable, or null when the user may post. */
@Composable
private fun postGateMessage(uiState: PoiDetailUiState, proximityKm: Int): String? = when {
    uiState.canPost -> null
    uiState.isGuest -> stringResource(R.string.poi_post_guest_prompt)
    !uiState.locationChecked -> null // still resolving location; show the button disabled
    uiState.distanceMeters == null -> stringResource(R.string.poi_post_location_unknown)
    else -> stringResource(R.string.poi_post_out_of_range, proximityKm)
}

/** The proximity-banner message and whether the user is in range (drives the accent). */
@Composable
private fun proximityMessage(uiState: PoiDetailUiState): Pair<String, Boolean> = when {
    uiState.isGuest -> stringResource(R.string.proximity_guest) to false
    // A favorite lets you post regardless of distance, so don't nag a favorited user to
    // "get closer" — they're always welcome here.
    uiState.isFavorited -> stringResource(R.string.proximity_favorited) to true
    !uiState.locationChecked -> stringResource(R.string.proximity_locating) to false
    uiState.distanceMeters == null -> stringResource(R.string.proximity_location_unknown) to false
    uiState.withinRange ->
        stringResource(R.string.proximity_in_range, formatDistance(uiState.distanceMeters!!)) to true
    else ->
        stringResource(R.string.proximity_out_of_range, formatDistance(uiState.distanceMeters!!)) to false
}

private fun typeIcon(type: String): ImageVector = when (type.lowercase()) {
    "park" -> Icons.Outlined.Park
    "college" -> Icons.Outlined.School
    "landmark" -> Icons.Outlined.AccountBalance
    else -> Icons.Outlined.Place
}
