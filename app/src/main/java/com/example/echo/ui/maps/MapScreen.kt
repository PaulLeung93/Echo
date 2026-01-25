package com.example.echo.ui.map

import android.Manifest
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
    var menuExpanded by remember { mutableStateOf(false) }
    var tagInput by remember { mutableStateOf("") }
    var filterAttempted by remember { mutableStateOf(false) }

    val defaultLocation = LatLng(40.7128, -74.0060)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val uiState by mapViewModel.uiState.collectAsState()

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

    // --- Layout ---
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- Top App Bar ---
            TopAppBar(
                title = {
                    if (uiState.currentTag != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Filtered: #${uiState.currentTag}",
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
                        uiState.clusters.forEach { cluster ->
                            val count = cluster.posts.size
                            if (count > 1) {
                                Marker(
                                    state = MarkerState(cluster.position),
                                    title = "$count posts",
                                    icon = BitmapDescriptorFactory.fromBitmap(createClusterIcon(context, count)),
                                    onClick = {
                                        mapViewModel.onClusterClick(cluster, cameraPositionState)
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

                        // POI markers
                        uiState.pois.forEach { poi ->
                            val latLng = LatLng(poi.latitude, poi.longitude)
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

                // Post preview card / Carousel
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
                                navController.navigate("${com.example.echo.utils.Constants.ROUTE_POST_DETAILS}/${post.id}")
                            },
                            onTagClick = { tag ->
                                tagInput = tag
                                mapViewModel.setTagFilter(tag, cameraPositionState)
                            }
                        )
                    }

                    // Update selected post when page changes
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
                            navController.navigate("${com.example.echo.utils.Constants.ROUTE_POST_DETAILS}/${post.id}")
                        },
                        onTagClick = { tag ->
                            tagInput = tag
                            mapViewModel.setTagFilter(tag, cameraPositionState)
                        },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                    )
                }
            }
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
