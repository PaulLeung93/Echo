package com.example.echo.ui.common

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Bottom navigation in the Neighborhood wireframe style: a warm cream bar with a
 * soft top shadow and rounded top corners. Tabs read as coral when selected
 * (filled icon + bold label) and muted brown when not — no Material pill
 * indicator. The center "Create" action is a raised coral circle.
 *
 * Layout (always 5 slots): Feed · Map · [Create] · Alerts · Profile.
 */
@Composable
fun BottomNavigationBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    isUserAuthenticated: Boolean,
    canCreate: Boolean,
    onCreateClick: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 10.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 14.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTab(
                label = "Feed",
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                selected = selectedTab == "feed",
                onClick = { onTabSelected("feed") }
            )
            NavTab(
                label = "Map",
                selectedIcon = Icons.Filled.Map,
                unselectedIcon = Icons.Outlined.Map,
                selected = selectedTab == "map",
                onClick = { onTabSelected("map") }
            )

            // Center "Create" — a raised coral circle with a white "+".
            CreateTab(
                enabled = canCreate,
                onClick = {
                    if (canCreate) {
                        onCreateClick()
                    } else {
                        Toast.makeText(context, "Please sign in to share an echo.", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            NavTab(
                label = "Alerts",
                selectedIcon = Icons.Filled.Notifications,
                unselectedIcon = Icons.Outlined.Notifications,
                selected = selectedTab == "alerts",
                onClick = {
                    if (isUserAuthenticated) {
                        onTabSelected("alerts")
                    } else {
                        Toast.makeText(context, "Please sign in to see your alerts.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
            NavTab(
                label = "Profile",
                selectedIcon = Icons.Filled.Person,
                unselectedIcon = Icons.Outlined.Person,
                selected = selectedTab == "profile",
                onClick = {
                    if (isUserAuthenticated) {
                        onTabSelected("profile")
                    } else {
                        Toast.makeText(context, "Please sign in to access your profile.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

/** A single tab: coral + bold + filled icon when selected, muted otherwise. */
@Composable
private fun RowScope.NavTab(
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/** Center create action: a raised coral circle with a white "+". */
@Composable
private fun RowScope.CreateTab(enabled: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
        Surface(
            shape = CircleShape,
            color = if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            },
            shadowElevation = 6.dp,
            modifier = Modifier
                .offset(y = (-12).dp)
                .size(52.dp)
                .clickable(onClick = onClick)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Create post",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
