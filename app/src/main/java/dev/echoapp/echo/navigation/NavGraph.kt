package dev.echoapp.echo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.echoapp.echo.ui.auth.*
import dev.echoapp.echo.ui.alerts.AlertsScreen
import dev.echoapp.echo.ui.auth.CompleteProfileScreen
import dev.echoapp.echo.ui.create.CreatePostScreen
import dev.echoapp.echo.ui.feed.FeedScreen
import dev.echoapp.echo.ui.maps.MapScreen
import dev.echoapp.echo.ui.poi.PoiDetailScreen
import dev.echoapp.echo.ui.post.PostDetailScreen
import dev.echoapp.echo.ui.profile.EditProfileScreen
import dev.echoapp.echo.ui.profile.FollowListScreen
import dev.echoapp.echo.ui.profile.ProfileScreen
import dev.echoapp.echo.ui.profile.UserProfileScreen
import dev.echoapp.echo.ui.settings.SettingsScreen
import dev.echoapp.echo.utils.Constants

object Destinations {
    const val SIGN_IN = Constants.ROUTE_SIGN_IN
    const val SIGN_UP = Constants.ROUTE_SIGN_UP
    const val FEED = Constants.ROUTE_FEED
    const val CREATE_POST = Constants.ROUTE_CREATE_POST
    const val FORGOT_PASSWORD = Constants.ROUTE_FORGOT_PASSWORD
    const val POST_DETAILS = Constants.ROUTE_POST_DETAILS
    const val MAP = Constants.ROUTE_MAP
    const val PROFILE = Constants.ROUTE_PROFILE
    const val POI_DETAILS = Constants.ROUTE_POI_DETAILS
    const val ALERTS = Constants.ROUTE_ALERTS
    const val COMPLETE_PROFILE = Constants.ROUTE_COMPLETE_PROFILE
    const val EDIT_PROFILE = Constants.ROUTE_EDIT_PROFILE
    const val SETTINGS = Constants.ROUTE_SETTINGS
    const val USER_PROFILE = Constants.ROUTE_USER_PROFILE
    const val FOLLOW_LIST = Constants.ROUTE_FOLLOW_LIST
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
        // ... (existing routes)

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

        // Create Post Screen. Optional `poiId` puts it in POI-thread mode (location
        // snapped to the POI); absent/blank for an ordinary feed post.
        composable(
            route = "${Destinations.CREATE_POST}?poiId={poiId}",
            arguments = listOf(navArgument("poiId") { defaultValue = "" })
        ) {
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
        ) { _ ->
            PostDetailScreen(navController = navController)
        }
        
        // POI Detail Screen
        composable(
            route = "${Destinations.POI_DETAILS}/{poiId}",
            arguments = listOf(
                navArgument("poiId") { defaultValue = "" }
            )
        ) { _ ->
            PoiDetailScreen(navController = navController)
        }

        // Map Screen
        composable(Destinations.MAP) {
            MapScreen(navController = navController)
        }

        // Profile Screen
        composable(Destinations.PROFILE) {
            ProfileScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }

        // Alerts Screen
        composable(Destinations.ALERTS) {
            AlertsScreen(navController = navController)
        }

        // Complete Profile (post-sign-up: name + username)
        composable(Destinations.COMPLETE_PROFILE) {
            CompleteProfileScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }

        // Edit Profile (name + bio)
        composable(Destinations.EDIT_PROFILE) {
            EditProfileScreen(navController = navController)
        }

        // Settings (appearance, notifications, account)
        composable(Destinations.SETTINGS) {
            SettingsScreen(navController = navController, authViewModel = authViewModel, webClientId = webClientId)
        }

        // Public profile for any user (reached by tapping an author).
        composable(
            route = "${Destinations.USER_PROFILE}/{uid}",
            arguments = listOf(navArgument("uid") { defaultValue = "" })
        ) {
            UserProfileScreen(navController = navController)
        }

        // Follower / following list for a user (reached by tapping a follow count).
        composable(
            route = "${Destinations.FOLLOW_LIST}/{uid}/{type}",
            arguments = listOf(
                navArgument("uid") { defaultValue = "" },
                navArgument("type") { defaultValue = "followers" }
            )
        ) {
            FollowListScreen(navController = navController)
        }
    }
}
