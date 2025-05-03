package com.example.echo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.echo.ui.auth.*
import com.example.echo.ui.create.CreatePostScreen
import com.example.echo.ui.feed.FeedScreen
import com.example.echo.ui.map.MapScreen
import com.example.echo.ui.post.PostDetailScreen
import com.example.echo.ui.profile.ProfileScreen
import com.example.echo.utils.Constants

object Destinations {
    const val SIGN_IN = Constants.ROUTE_SIGN_IN
    const val SIGN_UP = Constants.ROUTE_SIGN_UP
    const val FEED = Constants.ROUTE_FEED
    const val CREATE_POST = Constants.ROUTE_CREATE_POST
    const val FORGOT_PASSWORD = Constants.ROUTE_FORGOT_PASSWORD
    const val POST_DETAILS = Constants.ROUTE_POST_DETAILS
    const val MAP = Constants.ROUTE_MAP
    const val PROFILE = Constants.ROUTE_PROFILE

}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    webClientId: String,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Sign In Screen
        composable(
            route = "${Destinations.SIGN_IN}?successMessage={successMessage}",
            arguments = listOf(
                navArgument("successMessage") { defaultValue = "" }
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

        // Feed Screen
        composable(Destinations.FEED) {
            FeedScreen(navController = navController, authViewModel = authViewModel)
        }

        // Sign Up Screen
        composable(Destinations.SIGN_UP) {
            SignUpScreen(navController = navController)
        }

        // Create Post Screen
        composable(Destinations.CREATE_POST) {
            CreatePostScreen(navController = navController)
        }

        // Forgot Password Screen
        composable(Destinations.FORGOT_PASSWORD) {
            ForgotPasswordScreen(navController = navController)
        }

        // Post Detail Screen with postId
        composable(
            route = "${Destinations.POST_DETAILS}/{postId}",
            arguments = listOf(
                navArgument("postId") { defaultValue = "" }
            )
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            PostDetailScreen(postId = postId, navController = navController)
        }

        //Msp Screen
        composable(Destinations.MAP) {
            MapScreen(navController = navController)
        }

        // Profile Screen
        composable(Destinations.PROFILE) {
            ProfileScreen(navController = navController, authViewModel = authViewModel)
        }

    }
}
