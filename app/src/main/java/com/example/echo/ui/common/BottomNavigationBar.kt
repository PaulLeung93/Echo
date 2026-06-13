package com.example.echo.ui.common

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun BottomNavigationBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    isUserAuthenticated: Boolean,
    canCreate: Boolean,
    onCreateClick: () -> Unit
) {
    val context = LocalContext.current

    val tabColors = NavigationBarItemDefaults.colors(
        unselectedIconColor = Color.White,
        unselectedTextColor = Color.White,
        selectedIconColor = Color.White,
        selectedTextColor = Color.White
    )

    NavigationBar(containerColor = MaterialTheme.colorScheme.primary) {
        NavigationBarItem(
            selected = selectedTab == "feed",
            onClick = { onTabSelected("feed") },
            icon = { Icon(Icons.Default.Home, contentDescription = "Feed") },
            label = { Text("Feed") },
            colors = tabColors
        )
        NavigationBarItem(
            selected = selectedTab == "map",
            onClick = { onTabSelected("map") },
            icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
            label = { Text("Map") },
            colors = tabColors
        )

        // Center "Create" action — a white circle with a coral "+", non-guests only.
        if (canCreate) {
            NavigationBarItem(
                selected = false,
                onClick = onCreateClick,
                icon = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.onPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Create post",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                label = { Text("Create") },
                colors = tabColors
            )
        }

        NavigationBarItem(
            selected = selectedTab == "profile",
            onClick = {
                if (isUserAuthenticated) {
                    onTabSelected("profile")
                } else {
                    Toast.makeText(context, "Please sign in to access your profile.", Toast.LENGTH_SHORT).show()
                }
            },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile") },
            colors = tabColors
        )
    }
}
