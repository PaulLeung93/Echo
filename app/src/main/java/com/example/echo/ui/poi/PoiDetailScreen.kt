package com.example.echo.ui.poi

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.echo.R
import com.example.echo.components.CommentCard
import com.example.echo.utils.Constants
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoiDetailScreen(
    navController: NavHostController,
    viewModel: PoiDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var newComment by remember { mutableStateOf("") }
    var commentJustAdded by remember { mutableStateOf(false) }

    // Lightweight client-side rate limiting (real enforcement is server-side).
    val commentTimestamps = remember { mutableStateListOf<Long>() }
    val maxComments = 5
    val windowMs = 30_000L
    val rateLimitedMessage = stringResource(R.string.comment_rate_limited)
    val proximityKm = (Constants.PROXIMITY_RADIUS_METERS / 1000).toInt()

    // Surface transient action errors (failed add/delete) as snackbars.
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Scroll to the newest comment after one is added.
    LaunchedEffect(uiState.comments.size, commentJustAdded) {
        if (commentJustAdded && uiState.comments.isNotEmpty()) {
            listState.animateScrollToItem(uiState.comments.size)
            commentJustAdded = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.poi_details_title), color = MaterialTheme.colorScheme.onPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            when {
                uiState.isLoading && uiState.poi == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = uiState.error ?: stringResource(R.string.unknown_error),
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                uiState.poi != null -> {
                    val poi = uiState.poi!!
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = poi.name,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    SuggestionChip(
                                        onClick = { },
                                        label = { Text(poi.type.replaceFirstChar { it.uppercase() }) }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = poi.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text(
                                text = stringResource(R.string.comments_header),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (uiState.comments.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.no_comments_yet),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            items(uiState.comments, key = { it.id }) { comment ->
                                val isOwnComment = uiState.currentUserId != null &&
                                    comment.authorId.isNotEmpty() &&
                                    comment.authorId == uiState.currentUserId
                                CommentCard(
                                    comment = comment,
                                    onDelete = if (isOwnComment) {
                                        { viewModel.deleteComment(comment.id) }
                                    } else null
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    CommentComposer(
                        canComment = uiState.canComment,
                        gateMessage = commentGateMessage(uiState, proximityKm),
                        value = newComment,
                        onValueChange = { newComment = it },
                        onSend = {
                            val now = System.currentTimeMillis()
                            val recent = commentTimestamps.filter { now - it < windowMs }
                            if (recent.size >= maxComments) {
                                scope.launch { snackbarHostState.showSnackbar(rateLimitedMessage) }
                            } else if (newComment.isNotBlank()) {
                                commentTimestamps.add(now)
                                viewModel.addComment(newComment) {
                                    newComment = ""
                                    commentJustAdded = true
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

/** Returns the reason commenting is unavailable, or null when the user may comment. */
@Composable
private fun commentGateMessage(uiState: PoiDetailUiState, proximityKm: Int): String? = when {
    uiState.canComment -> null
    uiState.isGuest -> stringResource(R.string.comment_guest_prompt)
    !uiState.locationChecked -> null // still resolving location; show the input disabled
    uiState.distanceMeters == null -> stringResource(R.string.comment_location_unknown)
    else -> stringResource(R.string.comment_out_of_range, proximityKm)
}

@Composable
private fun CommentComposer(
    canComment: Boolean,
    gateMessage: String?,
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)) {
        if (gateMessage != null) {
            Text(
                text = gateMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(stringResource(R.string.write_comment_hint)) },
                enabled = canComment,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onSend,
                enabled = canComment && value.isNotBlank()
            ) {
                Text(stringResource(R.string.send))
            }
        }
    }
}
