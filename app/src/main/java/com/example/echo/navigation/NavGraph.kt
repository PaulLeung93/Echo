package com.example.echo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.echo.ui.auth.SignInScreen
import com.example.echo.ui.auth.SignUpScreen
import com.example.echo.ui.auth.AuthViewModel
import androidx.navigation.navArgument


object Destinations {
    const val SIGN_IN = "sign_in"
    const val EMAIL_SIGN_IN = "email_sign_in"
    const val SIGN_UP = "sign_up"
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    webClientId: String
) {
    NavHost(
        navController = navController,
        startDestination = Destinations.SIGN_IN
    ) {
        composable(
            route = "${Destinations.SIGN_IN}?successMessage={successMessage}",
            arguments = listOf(
                navArgument("successMessage") {
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val successMessage = backStackEntry.arguments?.getString("successMessage") ?: ""
            SignInScreen(
                authViewModel = authViewModel,
                webClientId = webClientId,
                navController = navController,
                successMessage = successMessage
            )
        }

        composable(Destinations.SIGN_UP) {
            SignUpScreen(navController)
        }
    }
}
