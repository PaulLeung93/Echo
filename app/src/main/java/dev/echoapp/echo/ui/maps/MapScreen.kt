package dev.echoapp.echo.ui.maps

import android.Manifest
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import dev.echoapp.echo.R
import dev.echoapp.echo.components.DeletePostDialog
import dev.echoapp.echo.components.EditPostDialog
import dev.echoapp.echo.components.PostCard
import dev.echoapp.echo.domain.model.Coordinates
import dev.echoapp.echo.domain.model.Post
import dev.echoapp.echo.navigation.Destinations
import dev.echoapp.echo.ui.common.TopSnackbarHost
import dev.echoapp.echo.utils.Constants
import dev.echoapp.echo.utils.distanceMeters
import dev.echoapp.echo.utils.formatDistance
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import android.os.Looper
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToInt

import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavHostController,
    mapViewModel: MapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    // Guards the one-time auto-center: with live updates streaming in, we must center
    // only on the *first* fix, or the camera would snap back every update and fight
    // the user panning around.
    var hasCenteredOnUser by rememberSaveable { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var showTagDialog by remember { mutableStateOf(false) }
    var showTypeDialog by remember { mutableStateOf(false) }
    var tagInput by remember { mutableStateOf("") }
    var filterAttempted by remember { mutableStateOf(false) }
    // Own posts pending an edit / delete from a map card.
    var postToEdit by remember { mutableStateOf<Post?>(null) }
    var postToDelete by remember { mutableStateOf<Post?>(null) }

    // A pending "open this post on the map" request from the feed (see MapFocusManager).
    val mapFocus by mapViewModel.mapFocus.collectAsStateWithLifecycle()
    // Whether this entry was opened by a focus request, captured once at composition so
    // the user-location auto-center below doesn't fight the focus animation.
    val openedWithFocus = remember { mapViewModel.mapFocus.value != null }

    val defaultLocation = LatLng(40.7128, -74.0060)
    val cameraPositionState = rememberCameraPositionState {
        // On a fresh focus entry, start already centered on the post (zoomed in past
        // MIN_POSTS_ZOOM so its marker loads) instead of the default view.
        val initialFocus = mapViewModel.mapFocus.value
        position = CameraPosition.fromLatLngZoom(
            initialFocus?.let { LatLng(it.latitude, it.longitude) } ?: defaultLocation,
            if (initialFocus != null) 16f else 12f
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uiState by mapViewModel.uiState.collectAsStateWithLifecycle()
    // Per-POI "last viewed" times; a POI's activity glow clears once its thread is
    // opened. Collected here so returning from a POI detail re-renders its marker.
    val poiViewedAt by mapViewModel.poiViewedAt.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        mapViewModel.uiEvent.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    // Captured for use inside the GoogleMap content lambda (no MaterialTheme there).
    val rippleColor = MaterialTheme.colorScheme.primary

    // Bucket the live zoom to whole levels so POI disc sizes track zoom without
    // re-rasterizing every camera frame. derivedStateOf only notifies readers when
    // the rounded level actually changes.
    val zoomBucket by remember {
        derivedStateOf { cameraPositionState.position.zoom.roundToInt() }
    }


    // Stream the user's location while the map is in the foreground so the ripple (and
    // the recenter target) track movement instead of being pinned to the launch fix.
    // Tied to the STARTED lifecycle state: updates stop when the map is backgrounded or
    // left, and resume on return. Balanced-power priority keeps the battery cost low.
    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (!locationPermissionState.status.isGranted) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Seed immediately from the last known fix so the ripple appears without
            // waiting for the first streamed update.
            try {
                fusedLocationClient.lastLocation.await()?.let {
                    userLocation = LatLng(it.latitude, it.longitude)
                }
            } catch (e: Exception) {
                Log.e("MapScreen", "Failed to get last known location", e)
            }

            val request = LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                5_000L
            ).setMinUpdateDistanceMeters(10f).build()
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let {
                        userLocation = LatLng(it.latitude, it.longitude)
                    }
                }
            }
            try {
                fusedLocationClient.requestLocationUpdates(
                    request, callback, Looper.getMainLooper()
                )
                awaitCancellation()
            } catch (e: SecurityException) {
                Log.e("MapScreen", "Location permission revoked", e)
            } finally {
                fusedLocationClient.removeLocationUpdates(callback)
            }
        }
    }

    // Center camera on the user's location once, on the first fix — unless we were
    // opened to focus on a specific post (that location wins). Only the first fix
    // centers, so subsequent live updates don't yank the camera back while panning.
    LaunchedEffect(userLocation) {
        if (!openedWithFocus && !hasCenteredOnUser) {
            userLocation?.let {
                hasCenteredOnUser = true
                cameraPositionState.animateSafely(CameraUpdateFactory.newLatLngZoom(it, 14f))
            }
        }
    }

    // Update zoom in ViewModel for clustering
    LaunchedEffect(cameraPositionState.position.zoom) {
        mapViewModel.updateZoom(cameraPositionState.position.zoom)
    }

    // Re-query viewport posts each time the map regains focus. The post source is a
    // one-shot viewport query, so without this a post created (or liked/edited/deleted)
    // on another screen wouldn't appear until the user panned outside the cached bounds.
    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            mapViewModel.refresh()
        }
    }

    // Center on a focused post's location. Also covers a *restored* map entry (whose
    // saved camera ignores the initial position above), so the camera always lands on
    // the post regardless of whether its marker has loaded yet.
    LaunchedEffect(mapFocus) {
        mapFocus?.let {
            cameraPositionState.animateSafely(
                CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 16f)
            )
        }
    }

    // Once the focused post shows up in the viewport-loaded posts, select it (popping
    // its bottom card) and consume the request. Consuming clears [mapFocus] so this
    // won't re-fire — and a later focus request will select again.
    LaunchedEffect(mapFocus, uiState.posts) {
        val focus = mapFocus ?: return@LaunchedEffect
        val post = uiState.posts.find { it.id == focus.postId } ?: return@LaunchedEffect
        mapViewModel.setSelectedPost(post, cameraPositionState)
        mapViewModel.consumeMapFocus()
    }

    // Cull markers to the viewport: whenever the camera comes to rest, hand its
    // visible bounds to the ViewModel so only on-screen posts/POIs get clustered and
    // drawn. `projection` is null until the first frame is laid out; until then the
    // VM shows everything, so there's no regression on first load.
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            cameraPositionState.projection?.visibleRegion?.latLngBounds
                ?.let(mapViewModel::updateVisibleBounds)
        }
    }

    // Notify user if no search results found
    LaunchedEffect(uiState.posts, filterAttempted) {
        if (filterAttempted && uiState.posts.isEmpty()) {
            snackbarHostState.showSnackbar("No posts found for \"$tagInput\"")
            filterAttempted = false
        }
    }

    // --- Layout: full-screen map with floating controls ---
    Box(modifier = Modifier.fillMaxSize()) {
        if (locationPermissionState.status.isGranted) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = true,
                    mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style)
                ),
                uiSettings = MapUiSettings(
                    compassEnabled = false,
                    zoomControlsEnabled = false,
                    // Use our own bottom-right recenter button instead of the
                    // native top-right one (which collides with the search bar).
                    myLocationButtonEnabled = false
                ),
                onMapClick = { mapViewModel.clearSelectedPost() },
                // Push the initial bounds as soon as the map is drawn, so posts load
                // even if the camera never moves (projection is null before this).
                onMapLoaded = {
                    cameraPositionState.projection?.visibleRegion?.latLngBounds
                        ?.let(mapViewModel::updateVisibleBounds)
                }
            ) {
                // Pulsing coral ripple at the user's location. Isolated in its own
                // composable so its ~60fps animation recomposes only itself — not the
                // markers below. That churn was rebuilding marker state/icons every
                // frame and making taps miss.
                userLocation?.let { loc ->
                    UserLocationRipple(center = loc, color = rippleColor)
                }

                // Cluster markers for user posts
                uiState.clusters.forEach { cluster ->
                    key(cluster.position.latitude, cluster.position.longitude) {
                        val count = cluster.posts.size
                        val markerState = rememberUpdatedMarkerState(position = cluster.position)
                        if (count > 1) {
                            val isSelected = uiState.selectedCluster?.position == cluster.position
                            val icon = remember(count, isSelected) {
                                BitmapDescriptorFactory.fromBitmap(
                                    createClusterIcon(context, count, scale = if (isSelected) 1.5f else 1.0f)
                                )
                            }
                            Marker(
                                state = markerState,
                                title = "$count posts",
                                icon = icon,
                                onClick = {
                                    mapViewModel.onClusterClick(cluster, cameraPositionState)
                                    true
                                }
                            )
                        } else {
                            val post = cluster.posts.first()
                            val isSelected = uiState.selectedPost?.id == post.id
                            // Same circular-disc convention as POI markers: coral POST
                            // disc + speech-bubble glyph, zoom-scaled, center-anchored.
                            val icon = remember(isSelected, zoomBucket) {
                                val zoomScale = markerScaleForZoom(zoomBucket)
                                poiPinDescriptor(
                                    PinCategory.POST,
                                    scale = if (isSelected) zoomScale * 1.5f else zoomScale
                                )
                            }
                            Marker(
                                state = markerState,
                                // Round disc (no pointer) → anchor at its center.
                                anchor = Offset(0.5f, 0.5f),
                                title = post.username,
                                snippet = post.message,
                                onClick = {
                                    mapViewModel.setSelectedPost(post, cameraPositionState)
                                    true
                                },
                                icon = icon
                            )
                        }
                    }
                }

                // POI markers
                uiState.pois.forEach { poi ->
                    key(poi.id) {
                        val latLng = LatLng(poi.latitude, poi.longitude)
                        val isSelected = uiState.selectedPoi?.id == poi.id
                        val markerState = rememberUpdatedMarkerState(position = latLng)
                        // Glow if this POI has a recent echo the user hasn't seen yet
                        // (clears once they've opened its thread). Evaluated per
                        // (re)composition — refreshes on data/zoom/selection/view changes.
                        val glowing = isPoiGlowing(poi.lastPostAt, poiViewedAt[poi.id] ?: 0L)
                        val icon = remember(poi.type, isSelected, zoomBucket, glowing) {
                            val zoomScale = markerScaleForZoom(zoomBucket)
                            poiPinDescriptor(
                                PinCategory.fromPoiType(poi.type),
                                scale = if (isSelected) zoomScale * 1.5f else zoomScale,
                                glowing = glowing
                            )
                        }
                        Marker(
                            state = markerState,
                            // Round disc (no pointer) → anchor at its center, not bottom.
                            anchor = Offset(0.5f, 0.5f),
                            title = poi.name,
                            snippet = "${poi.description} • ${poi.postCount} echoes",
                            onClick = {
                                mapViewModel.setSelectedPoi(poi, cameraPositionState)
                                true
                            },
                            icon = icon
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Location permission is required to show your current location.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { locationPermissionState.launchPermissionRequest() }) {
                    Text("Grant Permission")
                }
            }
        }

        // --- Floating search + filter bar ---
        MapSearchBar(
            activeTag = uiState.currentTag,
            onSearchClick = { showTagDialog = true },
            onClearTag = {
                mapViewModel.clearTagFilter()
                tagInput = ""
            },
            onFilterClick = { showTypeDialog = true },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(16.dp)
        )

        // --- "Zoom in to see posts" hint when zoomed out past the load threshold ---
        AnimatedVisibility(
            visible = uiState.postsZoomedOut,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 84.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shadowElevation = 3.dp
            ) {
                Text(
                    text = "Zoom in to see posts",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // --- Recenter button (bottom-right; lifts above a selection card) ---
        if (locationPermissionState.status.isGranted) {
            val hasSelection = uiState.selectedPost != null ||
                uiState.selectedCluster != null ||
                uiState.selectedPoi != null
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = if (hasSelection) 200.dp else 16.dp)
                    .size(48.dp)
                    .clickable {
                        scope.launch {
                            // Request a fresh high-accuracy fix on tap so the recenter
                            // lands on where the user is *now*, not a stale cached fix
                            // (e.g. while moving on a train). Fall back to the latest
                            // streamed location if the one-shot can't resolve in time.
                            val fresh = if (locationPermissionState.status.isGranted) {
                                try {
                                    fusedLocationClient.getCurrentLocation(
                                        Priority.PRIORITY_HIGH_ACCURACY,
                                        CancellationTokenSource().token
                                    ).await()?.let { LatLng(it.latitude, it.longitude) }
                                } catch (e: Exception) {
                                    null
                                }
                            } else null
                            val target = fresh ?: userLocation
                            if (target != null) {
                                userLocation = target
                                cameraPositionState.animateSafely(
                                    CameraUpdateFactory.newLatLngZoom(target, 14f)
                                )
                            }
                        }
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = "Recenter on my location",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // --- Selected post / cluster / POI card overlays ---
        uiState.selectedCluster?.let { cluster ->
            val pagerState = rememberPagerState(pageCount = { cluster.posts.size })

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                contentPadding = PaddingValues(horizontal = 32.dp),
                pageSpacing = 16.dp
            ) { page ->
                val post = cluster.posts[page]
                val isOwnPost = !mapViewModel.isGuest && post.authorId.isNotBlank() &&
                    post.authorId == mapViewModel.currentUserId
                PostCard(
                    post = post,
                    isLiked = post.likedByCurrentUser,
                    likeCount = post.likeCount,
                    commentCount = post.commentCount,
                    onLikeClick = { mapViewModel.toggleLike(post.id) },
                    onClick = {
                        navController.navigate("${Constants.ROUTE_POST_DETAILS}/${post.id}")
                    },
                    onTagClick = { tag ->
                        tagInput = tag
                        mapViewModel.setTagFilter(tag, cameraPositionState)
                    },
                    onEdit = if (isOwnPost) {
                        { postToEdit = post }
                    } else null,
                    onDelete = if (isOwnPost) {
                        { postToDelete = post }
                    } else null
                )
            }

            LaunchedEffect(pagerState.currentPage) {
                mapViewModel.onSelectedPostChanged(cluster.posts[pagerState.currentPage])
            }
        } ?: uiState.selectedPost?.let { post ->
            val isOwnPost = !mapViewModel.isGuest && post.authorId.isNotBlank() &&
                post.authorId == mapViewModel.currentUserId
            PostCard(
                post = post,
                isLiked = post.likedByCurrentUser,
                likeCount = post.likeCount,
                commentCount = post.commentCount,
                onLikeClick = { mapViewModel.toggleLike(post.id) },
                onClick = {
                    navController.navigate("${Constants.ROUTE_POST_DETAILS}/${post.id}")
                },
                onTagClick = { tag ->
                    tagInput = tag
                    mapViewModel.setTagFilter(tag, cameraPositionState)
                },
                onEdit = if (isOwnPost) {
                    { postToEdit = post }
                } else null,
                onDelete = if (isOwnPost) {
                    { postToDelete = post }
                } else null,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
            )
        } ?: uiState.selectedPoi?.let { poi ->
            val distanceLabel = userLocation?.let {
                formatDistance(
                    distanceMeters(
                        Coordinates(it.latitude, it.longitude),
                        Coordinates(poi.latitude, poi.longitude)
                    )
                )
            }
            PoiPreviewCard(
                name = poi.name,
                description = poi.description,
                type = poi.type,
                postCount = poi.postCount,
                distanceLabel = distanceLabel,
                onClick = { navController.navigate("${Destinations.POI_DETAILS}/${poi.id}") },
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
            )
        }

        TopSnackbarHost(snackbarHostState = snackbarHostState)
    }

    postToEdit?.let { post ->
        EditPostDialog(
            initialText = post.message,
            onConfirm = { newMessage -> mapViewModel.updatePost(post.id, newMessage) },
            onDismiss = { postToEdit = null }
        )
    }

    postToDelete?.let { post ->
        DeletePostDialog(
            onConfirm = { mapViewModel.deletePost(post.id) },
            onDismiss = { postToDelete = null }
        )
    }

    // --- Tag filter prompt dialog ---
    if (showTagDialog) {
        AlertDialog(
            onDismissRequest = { showTagDialog = false },
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
                    filterAttempted = true
                    mapViewModel.setTagFilter(tagInput.trim().lowercase(), cameraPositionState)
                    showTagDialog = false
                }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    mapViewModel.clearTagFilter()
                    tagInput = ""
                    showTagDialog = false
                }) {
                    Text("Clear")
                }
            }
        )
    }

    // --- Marker type filter dialog ---
    if (showTypeDialog) {
        MarkerTypeFilterDialog(
            selectedTypes = uiState.activeFilters,
            onDismiss = { showTypeDialog = false },
            onApply = { selectedTypes ->
                mapViewModel.filterByMarkerTypes(selectedTypes)
            }
        )
    }
}

/**
 * Expanding, fading ripple drawn at [center]. Hosts its own infinite animation so
 * the per-frame recomposition stays scoped to this composable and never invalidates
 * the marker composables sitting alongside it in the map content (which would
 * otherwise rebuild marker state + icons every frame and make taps unreliable).
 */
@Composable
@GoogleMapComposable
private fun UserLocationRipple(center: LatLng, color: Color) {
    val transition = rememberInfiniteTransition(label = "mapRipple")
    val ripple by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 2000, easing = LinearEasing)),
        label = "ripple"
    )
    Circle(
        center = center,
        radius = 60.0 + 440.0 * ripple,
        strokeWidth = 0f,
        fillColor = color.copy(alpha = 0.5f * (1f - ripple))
    )
}

/** Floating search pill + circular filter button over the map (wireframe style). */
@Composable
private fun MapSearchBar(
    activeTag: String?,
    onSearchClick: () -> Unit,
    onClearTag: () -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
            modifier = Modifier
                .weight(1f)
                .clickable { onSearchClick() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(10.dp))
                if (activeTag != null) {
                    Text(
                        text = "#$activeTag",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear filter",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { onClearTag() }
                    )
                } else {
                    Text(
                        text = "Search your neighborhood…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
            modifier = Modifier
                .size(52.dp)
                .clickable { onFilterClick() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = "Filters",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Bottom card for a selected POI: type chip + distance, name, description, comments. */
@Composable
private fun PoiPreviewCard(
    name: String,
    description: String,
    type: String,
    postCount: Int,
    distanceLabel: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Text(
                        text = type.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
                if (distanceLabel != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "• $distanceLabel",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Comment,
                    contentDescription = "Echoes",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = postCount.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
