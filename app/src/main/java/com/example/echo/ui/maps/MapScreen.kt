package com.example.echo.ui.maps

import android.Manifest
import android.util.Log
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.echo.R
import com.example.echo.components.PostCard
import com.example.echo.domain.model.Coordinates
import com.example.echo.navigation.Destinations
import com.example.echo.ui.common.TopSnackbarHost
import com.example.echo.utils.Constants
import com.example.echo.utils.distanceMeters
import com.example.echo.utils.formatDistance
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
    var showTagDialog by remember { mutableStateOf(false) }
    var showTypeDialog by remember { mutableStateOf(false) }
    var tagInput by remember { mutableStateOf("") }
    var filterAttempted by remember { mutableStateOf(false) }

    val defaultLocation = LatLng(40.7128, -74.0060)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uiState by mapViewModel.uiState.collectAsState()

    // Captured for use inside the GoogleMap content lambda (no MaterialTheme there).
    val radiusStroke = MaterialTheme.colorScheme.primary
    val radiusFill = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    val rippleColor = MaterialTheme.colorScheme.primaryContainer

    // Pulsing "echo" ripple around the user's location (2s loop, fades out).
    val rippleTransition = rememberInfiniteTransition(label = "mapRipple")
    val ripple by rippleTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 2000, easing = LinearEasing)),
        label = "ripple"
    )

    // Get last known location on launch
    LaunchedEffect(Unit) {
        if (locationPermissionState.status.isGranted) {
            try {
                val location = fusedLocationClient.lastLocation.await()
                if (location != null) {
                    userLocation = LatLng(location.latitude, location.longitude)
                }
            } catch (e: Exception) {
                Log.e("MapScreen", "Failed to get location", e)
            }
        }
    }

    // Center camera on user location when acquired
    LaunchedEffect(userLocation) {
        userLocation?.let {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 14f))
        }
    }

    // Update zoom in ViewModel for clustering
    LaunchedEffect(cameraPositionState.position.zoom) {
        mapViewModel.updateZoom(cameraPositionState.position.zoom)
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
                    zoomControlsEnabled = false,
                    // Use our own bottom-right recenter button instead of the
                    // native top-right one (which collides with the search bar).
                    myLocationButtonEnabled = false
                ),
                onMapClick = { mapViewModel.clearSelectedPost() }
            ) {
                // The user's 5km interaction radius (where they can comment).
                userLocation?.let { loc ->
                    Circle(
                        center = loc,
                        radius = Constants.PROXIMITY_RADIUS_METERS,
                        strokeColor = radiusStroke,
                        strokeWidth = 3f,
                        fillColor = radiusFill
                    )
                    // Pulsing coral ripple at the user's location (expands + fades).
                    // Sized to read at neighbourhood zoom (meters don't scale with
                    // zoom, so this is a deliberate visible radius, not the 5km gate).
                    Circle(
                        center = loc,
                        radius = 60.0 + 440.0 * ripple,
                        strokeWidth = 0f,
                        fillColor = rippleColor.copy(alpha = 0.5f * (1f - ripple))
                    )
                }

                // Cluster markers for user posts
                uiState.clusters.forEach { cluster ->
                    val count = cluster.posts.size
                    if (count > 1) {
                        val isSelected = uiState.selectedCluster?.position == cluster.position
                        Marker(
                            state = MarkerState(cluster.position),
                            title = "$count posts",
                            icon = BitmapDescriptorFactory.fromBitmap(
                                createClusterIcon(
                                    context,
                                    count,
                                    scale = if (isSelected) 1.5f else 1.0f
                                )
                            ),
                            onClick = {
                                mapViewModel.onClusterClick(cluster, cameraPositionState)
                                true
                            }
                        )
                    } else {
                        val post = cluster.posts.first()
                        val isSelected = uiState.selectedPost?.id == post.id
                        Marker(
                            state = MarkerState(cluster.position),
                            title = post.username,
                            snippet = post.message,
                            onClick = {
                                mapViewModel.setSelectedPost(post, cameraPositionState)
                                true
                            },
                            icon = bitmapDescriptorFromVector(
                                context,
                                R.drawable.ic_default,
                                scale = if (isSelected) 1.5f else 1.0f
                            )
                        )
                    }
                }

                // POI markers
                uiState.pois.forEach { poi ->
                    val latLng = LatLng(poi.latitude, poi.longitude)
                    val isSelected = uiState.selectedPoi?.id == poi.id
                    Marker(
                        state = MarkerState(position = latLng),
                        title = poi.name,
                        snippet = "${poi.description} • ${poi.commentCount} comments",
                        onClick = {
                            mapViewModel.setSelectedPoi(poi, cameraPositionState)
                            true
                        },
                        icon = when (poi.type.lowercase()) {
                            "college" -> bitmapDescriptorFromVector(context, R.drawable.ic_college, scale = if (isSelected) 1.5f else 1.0f)
                            "park" -> bitmapDescriptorFromVector(context, R.drawable.ic_park, scale = if (isSelected) 1.5f else 1.0f)
                            "landmark" -> bitmapDescriptorFromVector(context, R.drawable.ic_landmark, scale = if (isSelected) 1.5f else 1.0f)
                            else -> BitmapDescriptorFactory.defaultMarker()
                        }
                    )
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
                            var target = userLocation
                            if (target == null && locationPermissionState.status.isGranted) {
                                target = try {
                                    fusedLocationClient.lastLocation.await()
                                        ?.let { LatLng(it.latitude, it.longitude) }
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            if (target != null) {
                                userLocation = target
                                cameraPositionState.animate(
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
                    }
                )
            }

            LaunchedEffect(pagerState.currentPage) {
                mapViewModel.onSelectedPostChanged(cluster.posts[pagerState.currentPage])
            }
        } ?: uiState.selectedPost?.let { post ->
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
                commentCount = poi.commentCount,
                distanceLabel = distanceLabel,
                onClick = { navController.navigate("${Destinations.POI_DETAILS}/${poi.id}") },
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
            )
        }

        TopSnackbarHost(snackbarHostState = snackbarHostState)
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
    commentCount: Int,
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
                    contentDescription = "Comments",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = commentCount.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
