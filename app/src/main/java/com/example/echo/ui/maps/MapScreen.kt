package com.example.echo.ui.map

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.echo.navigation.Destinations
import com.example.echo.ui.common.BottomNavigationBar
import com.example.echo.ui.auth.AuthViewModel
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
    val posts by mapViewModel.posts.collectAsState()

    val defaultLocation = LatLng(40.7128, -74.0060) // New York City fallback
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    }
    var selectedTab by remember { mutableStateOf("map") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Map") }
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
                    "profile" -> {
                        // Placeholder for ProfileScreen
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
            if (locationPermissionState.status.isGranted) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = false),
                    properties = MapProperties(isMyLocationEnabled = true)
                ) {
                    // Draw markers only for posts with valid lat/lng
                    posts.filter { it.latitude != null && it.longitude != null }.forEach { post ->
                        Marker(
                            state = rememberMarkerState(
                                position = LatLng(post.latitude!!, post.longitude!!)
                            ),
                            title = post.username,
                            snippet = post.message
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
        }
    }
}
