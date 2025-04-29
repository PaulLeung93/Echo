package com.example.echo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.echo.navigation.AppNavGraph
import com.example.echo.navigation.RootNavHost
import com.example.echo.ui.auth.AuthViewModel
import com.example.echo.ui.auth.SignInScreen
import com.example.echo.ui.theme.EchoTheme


class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            EchoTheme {
                val navController = rememberNavController()
                Surface(
                    modifier = Modifier.fillMaxSize(),
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

