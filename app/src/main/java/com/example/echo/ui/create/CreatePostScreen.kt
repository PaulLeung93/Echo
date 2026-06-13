package com.example.echo.ui.create

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.echo.navigation.Destinations
import kotlinx.coroutines.launch

import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    navController: NavHostController,
    viewModel: CreatePostViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var message by remember { mutableStateOf("") }

    var newTag by remember { mutableStateOf("") }
    val tags = remember { mutableStateListOf<String>() }
    val maxTags = 3

    // Handle success
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            navController.navigate(Destinations.FEED) {
                popUpTo(Destinations.FEED) { inclusive = true }
            }
        }
    }

    // Handle error
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Share an echo") }
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

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("What's happening around you?") },
                    maxLines = 5,
                    singleLine = false,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tag input
                OutlinedTextField(
                    value = newTag,
                    onValueChange = { newTag = it },
                    label = { Text("Add a tag (max 3)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tags.forEach { tag ->
                        AssistChip(
                            onClick = {},
                            label = { Text(tag) },
                            trailingIcon = {
                                IconButton(onClick = { tags.remove(tag) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove Tag")
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val trimmed = newTag.trim()
                        if (trimmed.isNotBlank() && trimmed.length <= 30 && !tags.contains(trimmed.lowercase())) {
                            if (tags.size < maxTags) {
                                tags.add(trimmed.lowercase())
                                newTag = ""
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("You can only add up to 3 tags.")
                                }
                            }
                        }
                    },
                    enabled = newTag.isNotBlank() && tags.size < maxTags
                ) {
                    Text("Add Tag")
                }

                // Include Location
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = uiState.includeLocation,
                        onCheckedChange = { viewModel.setIncludeLocation(it) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = "Include location")
                        when {
                            uiState.isLocationLoading -> Text(
                                text = "Getting your location…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            uiState.locationUnavailable -> Text(
                                text = "Location unavailable",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            uiState.includeLocation && uiState.latitude != null -> Text(
                                text = "Location attached",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Post Button
                Button(
                    onClick = {
                        viewModel.submitPost(message = message, tags = tags.toList())
                    },
                    enabled = !uiState.isLoading && !uiState.isLocationLoading,
                    shape = RoundedCornerShape(percent = 50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (uiState.isLoading) {
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
