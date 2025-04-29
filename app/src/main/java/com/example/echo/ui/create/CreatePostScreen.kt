package com.example.echo.ui.create

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.echo.navigation.Destinations
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission") // We'll manually check permission
@Composable
fun CreatePostScreen(
    navController: NavHostController,
    viewModel: CreatePostViewModel = viewModel()
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var message by remember { mutableStateOf("") }
    var includeLocation by remember { mutableStateOf(false) }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Post") }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                errorMessage?.let { error ->
                    LaunchedEffect(error) {
                        snackbarHostState.showSnackbar(error)
                        viewModel.clearError()
                    }
                }

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("What's on your mind?") },
                    maxLines = 5,
                    singleLine = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Include Location Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = includeLocation,
                        onCheckedChange = { checked ->
                            includeLocation = checked
                            if (checked) {
                                // Check location permission
                                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                                includeLocation = true
                                if (ActivityCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED ||
                                    ActivityCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                        if (location != null) {
                                            latitude = location.latitude
                                            longitude = location.longitude
                                        }
                                    }
                                }
                                // If no permission, do nothing (don't crash)
                            } else {
                                latitude = null
                                longitude = null
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Include Location")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Post Button
                Button(
                    onClick = {
                        viewModel.submitPost(
                            message,
                            includeLocation,
                            userLatitude = if (includeLocation) latitude else null,
                            userLongitude = if (includeLocation) longitude else null,
                        ) {
                            navController.navigate(Destinations.FEED) {
                                popUpTo(Destinations.FEED) { inclusive = true }
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text("Post")
                    }
                }
            }
        }
    }
}
