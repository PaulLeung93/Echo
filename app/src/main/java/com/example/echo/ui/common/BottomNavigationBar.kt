package com.example.echo.ui.common

import android.widget.Toast
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun BottomNavigationBar(selectedTab: String,
                        onTabSelected: (String) -> Unit,
                        isUserAuthenticated: Boolean
) {
    val context = LocalContext.current

    NavigationBar(containerColor = MaterialTheme.colorScheme.primary) {

        NavigationBarItem(
            selected = selectedTab == "feed",
            onClick = { onTabSelected("feed") },
            icon = { Icon(Icons.Default.Home, contentDescription = "Feed") },
            label = { Text("Feed") },
            colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = Color.White,
                unselectedTextColor = Color.White,
                selectedIconColor = Color.White,
                selectedTextColor = Color.White
            )
        )
        NavigationBarItem(
            selected = selectedTab == "map",
            onClick = { onTabSelected("map") },
            icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
            label = { Text("Map") },
            colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = Color.White,
                unselectedTextColor = Color.White,
                selectedIconColor = Color.White,
                selectedTextColor = Color.White
            )
        )
        NavigationBarItem(
            selected = selectedTab == "profile",
            onClick = {
                if (isUserAuthenticated) {
                    onTabSelected("profile")
                } else {
                    Toast.makeText(
                        context,
                        "Please sign in to access your profile.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile") },
            colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = Color.White,
                unselectedTextColor = Color.White,
                selectedIconColor = Color.White,
                selectedTextColor = Color.White
            )
        )
    }
}