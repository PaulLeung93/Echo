package dev.echoapp.echo.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.echoapp.echo.components.EmptyState
import dev.echoapp.echo.components.ProfileAvatar
import dev.echoapp.echo.domain.model.UserProfile
import dev.echoapp.echo.domain.usecase.follow.FollowListType
import dev.echoapp.echo.utils.Constants

/**
 * A two-tab list of a user's followers / who they follow. Each row links through to
 * that user's public profile. Read-only; no inline follow button in v1.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowListScreen(
    navController: NavHostController,
    viewModel: FollowListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedIndex = if (uiState.selectedType == FollowListType.FOLLOWERS) 0 else 1

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connections", color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedIndex) {
                Tab(
                    selected = selectedIndex == 0,
                    onClick = { viewModel.selectType(FollowListType.FOLLOWERS) },
                    text = { Text("Followers") }
                )
                Tab(
                    selected = selectedIndex == 1,
                    onClick = { viewModel.selectType(FollowListType.FOLLOWING) },
                    text = { Text("Following") }
                )
            }

            when {
                uiState.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                uiState.error != null -> EmptyState(
                    icon = Icons.Outlined.CloudOff,
                    title = "Couldn't load this list",
                    subtitle = uiState.error,
                    isError = true,
                    modifier = Modifier.fillMaxSize()
                )

                uiState.profiles.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.Group,
                    title = if (uiState.selectedType == FollowListType.FOLLOWERS) "No followers yet" else "Not following anyone yet",
                    subtitle = if (uiState.selectedType == FollowListType.FOLLOWERS) {
                        "When people follow this account, they'll show up here."
                    } else {
                        "People this account follows will show up here."
                    },
                    modifier = Modifier.fillMaxSize()
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(uiState.profiles, key = { it.uid }) { profile ->
                        FollowRow(
                            profile = profile,
                            onClick = { navController.navigate("${Constants.ROUTE_USER_PROFILE}/${profile.uid}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowRow(profile: UserProfile, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProfileAvatar(
            photoUrl = profile.photoUrl,
            name = profile.fullName.ifBlank { profile.username },
            size = 44.dp
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile.fullName.ifBlank { profile.username },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${profile.username}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
