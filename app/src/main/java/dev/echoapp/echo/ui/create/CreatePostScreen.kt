package dev.echoapp.echo.ui.create

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.echoapp.echo.navigation.Destinations
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.launch

import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalPermissionsApi::class)
@Composable
fun CreatePostScreen(
    navController: NavHostController,
    viewModel: CreatePostViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var message by remember { mutableStateOf("") }
    var newTag by remember { mutableStateOf("") }
    val tags = remember { mutableStateListOf<String>() }
    val maxTags = 3

    // Location permission, requested in-context when the user opts to share it.
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    var showLocationRationale by remember { mutableStateOf(false) }
    var hasAskedLocation by remember { mutableStateOf(false) }
    var pendingLocationRequest by remember { mutableStateOf(false) }

    // Once the user grants permission (from our request), turn location on.
    LaunchedEffect(locationPermission.status.isGranted) {
        if (locationPermission.status.isGranted && pendingLocationRequest) {
            pendingLocationRequest = false
            viewModel.setIncludeLocation(true)
        }
    }

    fun addTag() {
        val trimmed = newTag.trim().lowercase()
        when {
            trimmed.isBlank() -> {}
            trimmed.length > 30 -> {}
            tags.contains(trimmed) -> { newTag = "" }
            tags.size >= maxTags -> coroutineScope.launch {
                snackbarHostState.showSnackbar("You can only add up to 3 tags.")
            }
            else -> { tags.add(trimmed); newTag = "" }
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            navController.navigate(Destinations.FEED) {
                popUpTo(Destinations.FEED) { inclusive = true }
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Share an echo",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background, shadowElevation = 0.dp) {
                Button(
                    onClick = { viewModel.submitPost(message = message, tags = tags.toList()) },
                    enabled = message.isNotBlank() && !uiState.isLoading && !uiState.isLocationLoading,
                    shape = RoundedCornerShape(percent = 50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .height(54.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text("Post", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // --- Message ---
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                placeholder = { Text("What's happening around you?") },
                minLines = 4,
                maxLines = 8,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            )

            // --- Location card ---
            LocationCard(
                includeLocation = uiState.includeLocation,
                isLoading = uiState.isLocationLoading,
                unavailable = uiState.locationUnavailable,
                attached = uiState.includeLocation && uiState.latitude != null,
                onToggle = { checked ->
                    when {
                        !checked -> viewModel.setIncludeLocation(false)
                        locationPermission.status.isGranted -> viewModel.setIncludeLocation(true)
                        else -> showLocationRationale = true
                    }
                }
            )

            // --- Tags ---
            Text(
                text = "TAGS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                tags.forEach { tag ->
                    InputChip(
                        selected = false,
                        onClick = { tags.remove(tag) },
                        label = { Text("#$tag") },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove $tag",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
            if (tags.size < maxTags) {
                OutlinedTextField(
                    value = newTag,
                    onValueChange = { newTag = it },
                    placeholder = { Text("Add tag (max 3)") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { addTag() }),
                    trailingIcon = {
                        if (newTag.isNotBlank()) {
                            TextButton(onClick = { addTag() }) { Text("Add") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // --- Location permission rationale / settings prompt ---
    if (showLocationRationale) {
        val permanentlyDenied = hasAskedLocation && !locationPermission.status.shouldShowRationale
        AlertDialog(
            onDismissRequest = { showLocationRationale = false },
            icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
            title = { Text("Share your location?") },
            text = {
                Text(
                    if (permanentlyDenied) {
                        "Location is turned off for Echo. Enable it in Settings to tag where this echo is from."
                    } else {
                        "Echo uses your location to tag where this echo is from, so neighbors nearby can find it. It's only attached when you turn this on."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showLocationRationale = false
                    if (permanentlyDenied) {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                        )
                    } else {
                        hasAskedLocation = true
                        pendingLocationRequest = true
                        locationPermission.launchPermissionRequest()
                    }
                }) {
                    Text(if (permanentlyDenied) "Open Settings" else "Allow")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationRationale = false }) { Text("Not now") }
            }
        )
    }
}

/** Wireframe-style location card: pin + label + status, with a toggle switch. */
@Composable
private fun LocationCard(
    includeLocation: Boolean,
    isLoading: Boolean,
    unavailable: Boolean,
    attached: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Share your location",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val (statusText, statusColor) = when {
                    isLoading -> "Getting your location…" to MaterialTheme.colorScheme.onSurfaceVariant
                    unavailable -> "Location unavailable" to MaterialTheme.colorScheme.error
                    attached -> "Attached to this echo" to MaterialTheme.colorScheme.secondary
                    else -> "Let neighbors see where this echo is from" to MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor
                )
            }
            Switch(checked = includeLocation, onCheckedChange = onToggle)
        }
    }
}
