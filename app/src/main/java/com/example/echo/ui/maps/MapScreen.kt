package com.example.echo.ui.map

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.echo.models.Post
import com.example.echo.navigation.Destinations
import com.example.echo.ui.common.BottomNavigationBar
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavHostController,
    mapViewModel: MapViewModel = viewModel()
) {
    val locationPermissionState = rememberPermissionState(permission = Manifest.permission.ACCESS_FINE_LOCATION)
    val posts by mapViewModel.filteredPosts.collectAsState()
    val selectedPost by mapViewModel.selectedPost.collectAsState()
    val likesMap by mapViewModel.likesMap.collectAsState()
    val commentsMap by mapViewModel.commentsMap.collectAsState()

    val defaultLocation = LatLng(40.7128, -74.0060)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    }
    var selectedTab by remember { mutableStateOf("map") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var tagInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Map") },
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
                    "profile" -> { /* Placeholder */ }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (locationPermissionState.status.isGranted) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = false),
                    properties = MapProperties(isMyLocationEnabled = true)
                ) {
                    posts.filter { it.latitude != null && it.longitude != null }.forEach { post ->
                        Marker(
                            state = rememberMarkerState(
                                position = LatLng(post.latitude!!, post.longitude!!)
                            ),
                            title = post.username,
                            snippet = post.message,
                            onClick = {
                                mapViewModel.setSelectedPost(post)
                                true // false allows default marker click behavior
                            }
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Location permission required to show your current location.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { locationPermissionState.launchPermissionRequest() }) {
                        Text("Grant Permission")
                    }
                }
            }

            // Custom Info Panel
            selectedPost?.let { post ->
                val likes = likesMap[post.id] ?: 0
                val comments = commentsMap[post.id] ?: 0

                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(post.username, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(post.message, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            Icon(Icons.Default.ThumbUp, contentDescription = "Likes")
                            Spacer(Modifier.width(4.dp))
                            Text("$likes likes")
                            Spacer(Modifier.width(16.dp))
                            Icon(Icons.Default.Comment, contentDescription = "Comments")
                            Spacer(Modifier.width(4.dp))
                            Text("$comments comments")
                        }
                    }
                }
            }
        }
    }

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
                    mapViewModel.setTagFilter(tagInput.trim().lowercase())
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
