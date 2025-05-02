package com.example.echo.ui.map

import android.Manifest
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.echo.navigation.Destinations
import com.example.echo.ui.common.BottomNavigationBar
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.tasks.await
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.filled.Close
import com.example.echo.components.PostCard
import com.example.echo.ui.common.TopSnackbarHost


@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavHostController,
    mapViewModel: MapViewModel = viewModel()
) {
    // --- Permissions and User Location ---
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }




    // Fetch user's last known location from FusedLocationProviderClient
    LaunchedEffect(Unit) {

        //location permission before accessing location
        val permissionGranted = locationPermissionState.status.isGranted

        if (permissionGranted) {
            try {
                val location = fusedLocationClient.lastLocation.await()
                if (location != null) {
                    userLocation = LatLng(location.latitude, location.longitude)
                }
            } catch (e: SecurityException) {
                Log.e("MapScreen", "SecurityException: Missing location permission.")
            } catch (e: Exception) {
                Log.e("MapScreen", "Failed to get user location: ${e.message}")
            }
        } else {
            Log.w("MapScreen", "Location permission not granted.")
        }
    }


    // --- UI State ---
    val snackbarHostState = remember { SnackbarHostState() }
    val posts by mapViewModel.filteredPosts.collectAsState()
    val selectedPost by mapViewModel.selectedPost.collectAsState()
    val likesMap by mapViewModel.likesMap.collectAsState()
    val commentsMap by mapViewModel.commentsMap.collectAsState()
    val poiMarkers by mapViewModel.poiMarkers.collectAsState()
    val userLikes by mapViewModel.userLikes.collectAsState()
    val activeTag by mapViewModel.currentTagFilter.collectAsState()

    var selectedTab by remember { mutableStateOf("map") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var tagInput by remember { mutableStateOf("") }
    var filterAttempted by remember { mutableStateOf(false) }

    val defaultLocation = LatLng(40.7128, -74.0060) // NYC fallback
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    }




    // Move camera to user location after it's available
    LaunchedEffect(userLocation) {
        userLocation?.let {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 14f))
        }
    }

    LaunchedEffect(posts) {
        if (filterAttempted && posts.isEmpty()) {
            snackbarHostState.showSnackbar("No posts found for \"$tagInput\"")
        }
    }

    // --- Scaffold Layout ---
    Scaffold(
        snackbarHost = { TopSnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (activeTag != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Filtered: #$activeTag",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                           // Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = {
                                mapViewModel.clearTagFilter()
                                tagInput = ""
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Filter")
                            }
                        }
                    } else {
                        Text("Map")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter by Tag")
                    }
                }
            )

        },

        bottomBar = {
            BottomNavigationBar(selectedTab = selectedTab) { tab ->
                selectedTab = tab
                when (tab) {
                    "feed" -> navController.navigate(Destinations.FEED) {
                        popUpTo(Destinations.FEED) { inclusive = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                    "profile" -> { /* Coming soon */ }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- Google Map ---
            if (locationPermissionState.status.isGranted) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = false),
                    properties = MapProperties(isMyLocationEnabled = true),
                    onMapClick = {
                        mapViewModel.clearSelectedPost()
                    }
                ) {
                    // Post markers
                    posts.filter { it.latitude != null && it.longitude != null }.forEach { post ->
                        Marker(
                            state = MarkerState(LatLng(post.latitude!!, post.longitude!!)),
                            title = post.username,
                            snippet = post.message,
                            onClick = {
                                mapViewModel.setSelectedPost(post, cameraPositionState)
                                true
                            }
                        )
                    }

                    // POI markers (currently disabled, uncomment to re-enable)
                    /*
                    poiMarkers.forEach { poi ->
                        Marker(
                            state = rememberMarkerState(position = LatLng(poi.latitude, poi.longitude)),
                            title = poi.name,
                            icon = bitmapDescriptorFromVector(
                                context = LocalContext.current,
                                vectorResId = when (poi.type) {
                                    "university" -> R.drawable.ic_school_marker
                                    "park" -> R.drawable.ic_tree_marker
                                    else -> R.drawable.ic_location_pin
                                }
                            )
                        )
                    }
                    */
                }
            } else {
                // Permission rationale
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Location permission is required to show your current location.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { locationPermissionState.launchPermissionRequest() }) {
                        Text("Grant Permission")
                    }
                }
            }

            // --- Custom Info Panel ---
            selectedPost?.let { post ->
                PostCard(
                    post = post,
                    isLiked = userLikes.contains(post.id),
                    likeCount = likesMap[post.id] ?: 0,
                    commentCount = commentsMap[post.id] ?: 0,
                    onLikeClick = {
                        mapViewModel.toggleLike(post.id)
                    },
                    onClick = {
                        navController.navigate("${Destinations.POST_DETAILS}/${post.id}")
                    },
                    onTagClick = { tag ->
                        tagInput = tag
                        mapViewModel.setTagFilter(tag, userLocation, cameraPositionState)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        }
    }

    // --- Filter Tag Dialog ---
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
                    filterAttempted = true
                    mapViewModel.setTagFilter(
                        tag = tagInput.trim().lowercase(),
                        userLocation = userLocation,
                        camera = cameraPositionState
                    )
                    showFilterDialog = false
                }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    mapViewModel.clearTagFilter()
                    tagInput = ""
                    showFilterDialog = false
                }) {
                    Text("Clear")
                }
            }
        )
    }
}
