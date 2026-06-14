package com.example.echo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraph.Companion.findStartDestination
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
    val user = uiState.currentUser

    // Resolve where the app should start. For a signed-in, non-anonymous user we
    // must know whether they've set up a profile yet (FEED vs COMPLETE_PROFILE),
    // which is an async check — null means "still checking, show the splash".
    val resolvedStart: String? = when {
        user == null -> Destinations.SIGN_IN
        user.isAnonymous -> Destinations.FEED
        uiState.needsProfileSetup == null -> null
        uiState.needsProfileSetup == true -> Destinations.COMPLETE_PROFILE
        else -> Destinations.FEED
    }

    // The NavHost's start destination is fixed at first composition, so capture
    // the first resolved value. Runtime auth changes (sign in/up/out) are handled
    // by explicit navigation from the screens, not by changing this.
    var initialStart by remember { mutableStateOf(resolvedStart) }
    LaunchedEffect(resolvedStart) {
        if (initialStart == null && resolvedStart != null) {
            initialStart = resolvedStart
        }
    }

    // Reactively route to Sign In whenever the session ends at runtime (sign-out
    // from any screen, or an expired/revoked session) — the NavHost's start
    // destination only applies on first composition, so this handles the rest.
    val signedOut = user == null
    LaunchedEffect(signedOut) {
        if (signedOut && initialStart != null) {
            val current = navController.currentDestination?.route
            val onAuthScreen = current == null ||
                current.startsWith(Destinations.SIGN_IN) ||
                current == Destinations.SIGN_UP ||
                current == Destinations.FORGOT_PASSWORD
            if (!onAuthScreen) {
                navController.navigate(Destinations.SIGN_IN) {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    val start = initialStart
    if (start == null) {
        SplashScreen()
    } else {
        AppNavGraph(
            navController = navController,
            authViewModel = authViewModel,
            webClientId = webClientId,
            startDestination = start
        )
    }
}
