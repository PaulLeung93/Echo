package com.example.echo.ui.poi

import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.echo.R
import com.example.echo.components.CommentCard
import com.example.echo.domain.model.Poi
import com.example.echo.utils.Constants
import com.example.echo.utils.formatDistance
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoiDetailScreen(
    navController: NavHostController,
    viewModel: PoiDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var newComment by remember { mutableStateOf("") }
    var commentJustAdded by remember { mutableStateOf(false) }

    // Lightweight client-side rate limiting (real enforcement is server-side).
    val commentTimestamps = remember { mutableStateListOf<Long>() }
    val maxComments = 5
    val windowMs = 30_000L
    val rateLimitedMessage = stringResource(R.string.comment_rate_limited)
    val proximityKm = (Constants.PROXIMITY_RADIUS_METERS / 1000).toInt()

    // Surface transient action errors (failed add/delete) as snackbars.
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Scroll to the newest comment after one is added.
    LaunchedEffect(uiState.comments.size, commentJustAdded) {
        if (commentJustAdded && uiState.comments.isNotEmpty()) {
            listState.animateScrollToItem(uiState.comments.size)
            commentJustAdded = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PlaceDetailTopBar(
                onBack = { navController.popBackStack() },
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
            if (uiState.poi != null) {
                CommentComposer(
                    canComment = uiState.canComment,
                    gateMessage = commentGateMessage(uiState, proximityKm),
                    value = newComment,
                    onValueChange = { newComment = it },
                    onSend = {
                        val now = System.currentTimeMillis()
                        val recent = commentTimestamps.filter { now - it < windowMs }
                        if (recent.size >= maxComments) {
                            scope.launch { snackbarHostState.showSnackbar(rateLimitedMessage) }
                        } else if (newComment.isNotBlank()) {
                            commentTimestamps.add(now)
                            viewModel.addComment(newComment) {
                                newComment = ""
                                commentJustAdded = true
                            }
                        }
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
                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                Spacer(Modifier.height(8.dp))
                                HeroImage(poi)
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
                                Spacer(Modifier.height(28.dp))
                                EchoesHeader(count = uiState.comments.size)
                                Spacer(Modifier.height(16.dp))
                            }
                        }

                        if (uiState.comments.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.no_comments_yet),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 24.dp)
                                )
                            }
                        } else {
                            items(uiState.comments, key = { it.id }) { comment ->
                                val isOwnComment = uiState.currentUserId != null &&
                                    comment.authorId.isNotEmpty() &&
                                    comment.authorId == uiState.currentUserId
                                CommentCard(
                                    comment = comment,
                                    isAuthor = isOwnComment,
                                    onDelete = if (isOwnComment) {
                                        { viewModel.deleteComment(comment.id) }
                                    } else null,
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceDetailTopBar(onBack: () -> Unit, onShare: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.back),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            onClick = onBack
        )
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
private fun HeroImage(poi: Poi) {
    Box(
        modifier = Modifier
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
private fun EchoesHeader(count: Int) {
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
}

@Composable
private fun CommentComposer(
    canComment: Boolean,
    gateMessage: String?,
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // The parent (MainActivity Scaffold) already pads for the navigation
                // bar, so only handle the keyboard here — excluding the nav-bar inset
                // it overlaps — to avoid double-padding the bottom.
                .windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars))
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = { Text(stringResource(R.string.write_comment_hint)) },
                    enabled = canComment,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    maxLines = 4,
                    modifier = Modifier.weight(1f)
                )
                val sendEnabled = canComment && value.isNotBlank()
                Surface(
                    shape = CircleShape,
                    color = if (sendEnabled) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    },
                    shadowElevation = if (sendEnabled) 3.dp else 0.dp,
                    modifier = Modifier
                        .height(48.dp)
                        .clickable(enabled = sendEnabled, onClick = onSend)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.send),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Returns the reason commenting is unavailable, or null when the user may comment. */
@Composable
private fun commentGateMessage(uiState: PoiDetailUiState, proximityKm: Int): String? = when {
    uiState.canComment -> null
    uiState.isGuest -> stringResource(R.string.comment_guest_prompt)
    !uiState.locationChecked -> null // still resolving location; show the input disabled
    uiState.distanceMeters == null -> stringResource(R.string.comment_location_unknown)
    else -> stringResource(R.string.comment_out_of_range, proximityKm)
}

/** The proximity-banner message and whether the user is in range (drives the accent). */
@Composable
private fun proximityMessage(uiState: PoiDetailUiState): Pair<String, Boolean> = when {
    uiState.isGuest -> stringResource(R.string.proximity_guest) to false
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
