package com.example.echo.ui.map

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.echo.components.PostCard
import com.example.echo.navigation.Destinations
import com.example.echo.ui.common.BottomNavigationBar
import com.example.echo.ui.common.TopSnackbarHost
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch


@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavHostController,
    mapViewModel: MapViewModel = viewModel()
) {
    // --- Permissions and Location ---
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    // Fetch user's last known location
    LaunchedEffect(Unit) {
        if (locationPermissionState.status.isGranted) {
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
        }
    }

    // --- UI State ---
    val snackbarHostState = remember { SnackbarHostState() }
    val clusters by mapViewModel.clusterGroups.collectAsState()
    val selectedPost by mapViewModel.selectedPost.collectAsState()
    val likesMap by mapViewModel.likesMap.collectAsState()
    val commentsMap by mapViewModel.commentsMap.collectAsState()
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
    val filteredPosts by mapViewModel.filteredPosts.collectAsState()


    val coroutineScope = rememberCoroutineScope()

    // Move camera to user location once available
    LaunchedEffect(userLocation) {
        userLocation?.let {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 14f))
        }
    }

    // Show snackbar if filtering yields no results
    LaunchedEffect(clusters) {
        if (filterAttempted && clusters.all { it.posts.isEmpty() }) {
            snackbarHostState.showSnackbar("No posts found for \"$tagInput\"")
        }
    }

    LaunchedEffect(cameraPositionState.position.zoom, filteredPosts) {
        mapViewModel.refreshClusters(cameraPositionState.position.zoom, filteredPosts)
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
                if (tab == "feed") {
                    navController.navigate(Destinations.FEED) {
                        popUpTo(Destinations.FEED) { inclusive = true }
                        launchSingleTop = true
                        restoreState = true
                    }
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
                    // Render clustered or individual post markers
                    clusters.forEach { cluster ->
                        val count = cluster.posts.size
                        val icon = if (count > 1) {
                            BitmapDescriptorFactory.fromBitmap(createClusterIcon(context, count))
                        } else null

                        Marker(
                            state = MarkerState(cluster.position),
                            title = if (count == 1) cluster.posts.first().username else "$count posts",
                            snippet = if (count == 1) cluster.posts.first().message else null,
                            icon = icon,
                            onClick = {
                                if (count == 1) {
                                    mapViewModel.setSelectedPost(cluster.posts.first(), cameraPositionState)
                                } else {
                                    coroutineScope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(
                                                cluster.position,
                                                cameraPositionState.position.zoom + 2
                                            )
                                        )
                                    }
                                }
                                true
                            }

                        )
                    }
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

            // --- Info Panel for Selected Post ---
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

// --- Cluster Icon Drawing Helper ---
fun createClusterIcon(context: android.content.Context, count: Int): Bitmap {
    val radius = 40f
    val bitmap = Bitmap.createBitmap((radius * 2).toInt(), (radius * 2).toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(66, 133, 244) // Cluster circle color (Google blue)
    }
    canvas.drawCircle(radius, radius, radius, paint)

    paint.apply {
        color = Color.WHITE
        textSize = 36f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    val y = radius - ((paint.descent() + paint.ascent()) / 2)
    canvas.drawText(count.toString(), radius, y, paint)

    return bitmap
}
