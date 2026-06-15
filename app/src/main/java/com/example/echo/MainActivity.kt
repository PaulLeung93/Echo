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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.echo.data.preferences.UserPreferencesRepository
import com.example.echo.navigation.RootNavHost
import com.example.echo.ui.auth.AuthViewModel
import com.example.echo.ui.common.BottomNavigationBar
import com.example.echo.ui.theme.EchoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferences: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val darkMode by preferences.darkMode.collectAsState(initial = false)
            EchoTheme(darkTheme = darkMode) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val authViewModel: AuthViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                val authUiState by authViewModel.uiState.collectAsState()
                val isUserAuthenticated = authUiState.currentUser != null
                val canCreate = isUserAuthenticated && authUiState.currentUser?.isAnonymous != true

                Scaffold(
                    bottomBar = {
                        if (currentRoute in listOf("feed", "map", "alerts", "profile")) {
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
                                isUserAuthenticated = isUserAuthenticated,
                                canCreate = canCreate,
                                onCreateClick = { navController.navigate("create_post") }
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
                            webClientId = stringResource(R.string.default_web_client_id)
                        )
                    }
                }
            }
        }
    }
}
