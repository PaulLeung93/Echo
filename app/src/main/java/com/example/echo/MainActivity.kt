package com.example.echo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.echo.navigation.RootNavHost
import com.example.echo.ui.auth.AuthViewModel
import com.example.echo.ui.common.BottomNavigationBar
import com.example.echo.ui.theme.EchoTheme

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val viewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        viewModel.checkUserSession()

        setContent {
            EchoTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val isUserAuthenticated = authViewModel.isUserAuthenticated

                Scaffold(
                    bottomBar = {
                        if (currentRoute in listOf("feed", "map", "profile")) {
                            BottomNavigationBar(
                                selectedTab = currentRoute ?: "feed",
                                onTabSelected = { route ->
                                    if (route != currentRoute) {
                                        navController.navigate(route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                isUserAuthenticated = isUserAuthenticated
                            )
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding()),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        RootNavHost(
                            navController = navController,
                            authViewModel = authViewModel,
                            webClientId = "YOUR_WEB_CLIENT_ID"
                        )
                    }
                }
            }
        }
    }
}
