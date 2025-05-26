package com.example.echo.ui.map

import android.Manifest
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.echo.R
import com.example.echo.components.PostCard
import com.example.echo.navigation.Destinations
import com.example.echo.ui.common.TopSnackbarHost
import com.example.echo.ui.maps.MarkerTypeFilterDialog
import com.example.echo.ui.maps.bitmapDescriptorFromVector
import com.example.echo.ui.maps.createClusterIcon
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavHostController,
    mapViewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showTypeDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var tagInput by remember { mutableStateOf("") }
    var filterAttempted by remember { mutableStateOf(false) }

    // Marker type filter options - simplified for exact Firestore match
    var selectedMarkerTypes by remember {
        mutableStateOf(setOf("user posts", "landmark", "park", "college"))
    }

    val defaultLocation = LatLng(40.7128, -74.0060)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val uiState by mapViewModel.uiState.collectAsState()
    val selectedPost by mapViewModel.selectedPost.collectAsState()
    val clusterGroups by mapViewModel.clusterGroups.collectAsState()
    val poiMarkers by mapViewModel.poiMarkers.collectAsState()

    // Get last known location on launch
    LaunchedEffect(Unit) {
        if (locationPermissionState.status.isGranted) {
            try {
                val location = fusedLocationClient.lastLocation.await()
                userLocation = LatLng(location.latitude, location.longitude)
            } catch (e: SecurityException) {
                Log.e("MapScreen", "Missing location permission", e)
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

    // Initial post fetch
    LaunchedEffect(Unit) {
        mapViewModel.fetchPostsWithLocation()
    }

    // Watch for cluster updates and notify user if no results
    val effectiveZoom by remember { derivedStateOf { cameraPositionState.position.zoom } }
    LaunchedEffect(uiState, effectiveZoom) {
        (uiState as? MapUiState.Success)?.let {
            if (filterAttempted && it.filteredPosts.isEmpty()) {
                snackbarHostState.showSnackbar("No posts found for \"$tagInput\"")
            }
            mapViewModel.refreshClusters(effectiveZoom, it.filteredPosts)
        }
    }

    // --- Layout ---
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- Top App Bar ---
            TopAppBar(
                title = {
                    val state = uiState
                    if (state is MapUiState.Success && state.currentTag != null) {
                        // Show filtered tag in top bar with option to clear
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Filtered: #${state.currentTag}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            IconButton(
                                onClick = {
                                    mapViewModel.clearTagFilter()
                                    tagInput = ""
                                },
                                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Filter")
                            }
                        }
                    } else {
                        Text("Echo", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    // Dropdown menu for filter options
                    IconButton(onClick = { menuExpanded = true }, colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter Options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Filter by Tags") },
                            onClick = {
                                menuExpanded = false
                                showTagDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Filter by Marker Type") },
                            onClick = {
                                menuExpanded = false
                                showTypeDialog = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )

            // --- Google Map Layer ---
            Box(modifier = Modifier.weight(1f)) {
                if (locationPermissionState.status.isGranted) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(
                            isMyLocationEnabled = true,
                            mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style)
                        ),
                        uiSettings = MapUiSettings(zoomControlsEnabled = false),
                        onMapClick = { mapViewModel.clearSelectedPost() }
                    ) {
                        // Cluster markers for user posts
                        clusterGroups.forEach { cluster ->
                            val count = cluster.posts.size
                            if (count > 1) {
                                Marker(
                                    state = MarkerState(cluster.position),
                                    title = "$count posts",
                                    icon = BitmapDescriptorFactory.fromBitmap(createClusterIcon(context, count)),
                                    onClick = {
                                        coroutineScope.launch {
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newLatLngZoom(
                                                    cluster.position,
                                                    cameraPositionState.position.zoom + 2
                                                )
                                            )
                                        }
                                        true
                                    }
                                )
                            } else {
                                val post = cluster.posts.first()
                                Marker(
                                    state = MarkerState(cluster.position),
                                    title = post.username,
                                    snippet = post.message,
                                    onClick = {
                                        mapViewModel.setSelectedPost(post, cameraPositionState)
                                        true
                                    },
                                    icon = bitmapDescriptorFromVector(context, R.drawable.ic_default)
                                )
                            }
                        }

                        // POI markers (filtered by type)
                        poiMarkers.forEach { poi ->
                            val latLng = LatLng(poi.location.latitude, poi.location.longitude)
                            if (selectedMarkerTypes.contains(poi.type.lowercase())) {
                                Marker(
                                    state = MarkerState(position = latLng),
                                    title = poi.name,
                                    snippet = poi.description,
                                    icon = when (poi.type.lowercase()) {
                                        "college" -> bitmapDescriptorFromVector(context, R.drawable.ic_college)
                                        "park" -> bitmapDescriptorFromVector(context, R.drawable.ic_park)
                                        "landmark" -> bitmapDescriptorFromVector(context, R.drawable.ic_landmark)
                                        else -> BitmapDescriptorFactory.defaultMarker()
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Fallback UI if permission not granted
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

                // Post preview card (if marker tapped)
                (uiState as? MapUiState.Success)?.let { state ->
                    selectedPost?.let { post ->
                        PostCard(
                            post = post,
                            isLiked = state.userLikes.contains(post.id),
                            likeCount = state.postLikes[post.id] ?: 0,
                            commentCount = state.commentCount[post.id] ?: 0,
                            onLikeClick = { mapViewModel.toggleLike(post.id) },
                            onClick = {
                                navController.navigate("${Destinations.POST_DETAILS}/${post.id}")
                            },
                            onTagClick = { tag ->
                                tagInput = tag
                                mapViewModel.setTagFilter(tag, userLocation, cameraPositionState)
                            },
                            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                        )
                    }
                }
            }
        }

        // --- Snackbar overlay ---
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(top = 8.dp)
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
                    mapViewModel.setTagFilter(tagInput.trim().lowercase(), userLocation, cameraPositionState)
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
            selectedTypes = selectedMarkerTypes,
            onDismiss = { showTypeDialog = false },
            onApply = { selectedTypes ->
                selectedMarkerTypes = selectedTypes
                mapViewModel.filterByMarkerTypes(selectedTypes)
            }
        )
    }
}
