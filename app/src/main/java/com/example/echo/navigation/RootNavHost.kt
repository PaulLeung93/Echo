package com.example.echo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import com.example.echo.ui.auth.AuthViewModel
import com.example.echo.ui.splash.SplashScreen

@Composable
fun RootNavHost(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    webClientId: String
) {
    val uiState by authViewModel.uiState.collectAsState()

    // We can assume user is signed in if currentUser is not null
    // You might want to add an isInitialSessionCheckComplete flag to AuthUiState for better splash handling
    val startDestination = if (uiState.currentUser != null) {
        Destinations.FEED
    } else {
        Destinations.SIGN_IN
    }

    AppNavGraph(
        navController = navController,
        authViewModel = authViewModel,
        webClientId = webClientId,
        startDestination = startDestination
    )
}
