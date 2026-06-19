package com.example.echo.ui.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.echo.navigation.Destinations
import com.example.echo.ui.common.TopSnackbarHost
import com.example.echo.utils.isStrongPassword
import com.example.echo.utils.isValidEmail
import kotlinx.coroutines.launch

import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        authViewModel.uiEvent.collect { event ->
            when (event) {
                is AuthUiEvent.NavigateToCompleteProfile -> {
                    // New account → collect name + username next. Clear the auth
                    // back stack so Back can't return to sign-up/sign-in.
                    navController.navigate(Destinations.COMPLETE_PROFILE) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    }
                }
                is AuthUiEvent.NavigateToHome -> {
                    navController.navigate(Destinations.FEED) {
                        // Clear the entire auth back stack (incl. the sign-in screen
                        // underneath) so Feed becomes the root.
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    }
                }
                is AuthUiEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                else -> {}
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Signature Echo ripple watermark bleeding off the top-right corner, to
        // match the Stitch wireframe. Drawn in code (not an image asset): two big,
        // faint coral rings. The Canvas clips to its bounds so they fade off-screen,
        // and it's pointer-transparent so it never intercepts taps.
        val rippleColor = MaterialTheme.colorScheme.primaryContainer
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = rippleColor,
                radius = size.width * 0.85f,
                center = Offset(size.width, size.height * 0.08f),
                alpha = 0.06f,
                style = Stroke(width = 40.dp.toPx())
            )
            drawCircle(
                color = rippleColor,
                radius = size.width * 0.6f,
                center = Offset(size.width * 1.06f, size.height * 0.02f),
                alpha = 0.10f,
                style = Stroke(width = 26.dp.toPx())
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Create your account", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(
                "Join your neighborhood on Echo.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email address") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val icon = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                            IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(icon, contentDescription = null) }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm password") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val icon = if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) { Icon(icon, contentDescription = null) }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            val trimmedEmail = email.trim()
                            val trimmedPassword = password.trim()
                            val trimmedConfirmPassword = confirmPassword.trim()

                            if (!isValidEmail(trimmedEmail)) {
                                coroutineScope.launch { snackbarHostState.showSnackbar("Please enter a valid email address.") }
                                return@Button
                            }
                            if (trimmedPassword.isBlank() || trimmedConfirmPassword.isBlank()) {
                                coroutineScope.launch { snackbarHostState.showSnackbar("Password fields cannot be empty.") }
                                return@Button
                            }
                            if (trimmedPassword.length < 6) {
                                coroutineScope.launch { snackbarHostState.showSnackbar("Password must be at least 6 characters long.") }
                                return@Button
                            }
                            if (!isStrongPassword(trimmedPassword)) {
                                coroutineScope.launch { snackbarHostState.showSnackbar("Password must contain uppercase, lowercase, number, and special character.") }
                                return@Button
                            }
                            if (trimmedPassword != trimmedConfirmPassword) {
                                coroutineScope.launch { snackbarHostState.showSnackbar("Passwords do not match.") }
                                return@Button
                            }
                            authViewModel.signUpWithEmail(trimmedEmail, trimmedPassword)
                        },
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(percent = 50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
                        else Text("Sign up", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { navController.popBackStack() }) {
                Text("Already have an account? Sign in", color = MaterialTheme.colorScheme.secondary)
            }
        }
        TopSnackbarHost(snackbarHostState = snackbarHostState)

        // Drawn last so it sits above the scrollable Column — otherwise the
        // Column's verticalScroll intercepts the taps over the back button.
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}
