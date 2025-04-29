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
    val isSignedIn by authViewModel.isSignedIn.collectAsState()

    val startDestination = when (isSignedIn) {
        true -> Destinations.FEED
        false -> Destinations.SIGN_IN
        null -> null // while loading
    }

    if (startDestination == null) {
        SplashScreen()
    } else {
        AppNavGraph(
            navController = navController,
            authViewModel = authViewModel,
            webClientId = webClientId,
            startDestination = startDestination
        )
    }
}
