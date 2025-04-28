package com.example.echo.ui.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.example.echo.R
import com.example.echo.navigation.Destinations
import com.example.echo.ui.common.TopSnackbarHost
import com.example.echo.utils.isValidEmail
import com.example.echo.utils.mapFirebaseErrorMessage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    authViewModel: AuthViewModel,
    webClientId: String,
    navController: NavHostController,
    successMessage: String = ""
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val googleSignInClient = remember {
        com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(
            context,
            com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
            ).requestIdToken(webClientId)
                .requestEmail()
                .build()
        )
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        coroutineScope.launch {
            isLoading = true
            try {
                authViewModel.handleGoogleSignInResult(result)
                snackbarHostState.showSnackbar("Signed in with Google!")
                navController.navigate(Destinations.FEED) {
                    popUpTo(Destinations.SIGN_IN) { inclusive = true }
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Google Sign-In failed. Please try again.")
            } finally {
                isLoading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = {}
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.login_background),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Echo", fontSize = 48.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Stay Connected, Locally.", color = Color.White)
                    Spacer(modifier = Modifier.height(24.dp))

                    // Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(0.85f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val icon = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = icon, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.85f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        )
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Login Button
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                try {
                                    if (isValidEmail(email) && password.isNotBlank()) {
                                        when (val result = authViewModel.signInWithEmail(email, password)) {
                                            is SignInResult.Success -> {
                                                snackbarHostState.showSnackbar("Welcome back!")
                                                navController.navigate(Destinations.FEED) {
                                                    popUpTo(Destinations.SIGN_IN) { inclusive = true }
                                                }
                                            }
                                            is SignInResult.Error -> snackbarHostState.showSnackbar(
                                                mapFirebaseErrorMessage(result.message)
                                            )
                                        }
                                    } else {
                                        snackbarHostState.showSnackbar("Please enter valid credentials.")
                                    }
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.75f)
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        else Text("Login")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Forgot Password / Sign Up
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        TextButton(onClick = { navController.navigate(Destinations.FORGOT_PASSWORD) }) {
                            Text("Forgot Password?", textDecoration = TextDecoration.Underline, color = Color.White)
                        }
                        TextButton(onClick = { navController.navigate(Destinations.SIGN_UP) }) {
                            Text("Sign Up", textDecoration = TextDecoration.Underline, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Divider with OR
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        Divider(Modifier.weight(1f), color = Color.White)
                        Text("  OR  ", color = Color.White)
                        Divider(Modifier.weight(1f), color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Google Sign-In Button
                    GoogleSignInButton { googleSignInLauncher.launch(googleSignInClient.signInIntent) }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = {
                        authViewModel.signInAsGuest()
                        navController.navigate(Destinations.FEED) {
                            popUpTo(Destinations.SIGN_IN) { inclusive = true }
                        }
                    }) {
                        Text("Continue as Guest", color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "By continuing, you agree to Echo's Terms of Service and Privacy Policy.",
                        color = Color.White,
                        fontSize = 8.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
        TopSnackbarHost(snackbarHostState = snackbarHostState)
    }
}

@Composable
fun GoogleSignInButton(onClick: () -> Unit) {
    AndroidView(
        factory = { context ->
            com.google.android.gms.common.SignInButton(context).apply {
                setSize(com.google.android.gms.common.SignInButton.SIZE_WIDE)
                setColorScheme(com.google.android.gms.common.SignInButton.COLOR_LIGHT)
                setOnClickListener { onClick() }
            }
        },
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .height(50.dp)
    )
}