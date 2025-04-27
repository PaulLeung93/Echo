package com.example.echo.ui.auth

import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(
    authViewModel: AuthViewModel,
    webClientId: String,
    navController: NavHostController,
    successMessage: String = ""
) {
    val context = LocalContext.current
    val isSignedIn by authViewModel.isSignedIn.collectAsState()
    val NavyBlue = MaterialTheme.colorScheme.primary

    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Email/Password states
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Setup Google Sign-In Client
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isLoading = true
        coroutineScope.launch {
            try {
                authViewModel.handleGoogleSignInResult(result)
            } catch (e: Exception) {
                errorMessage = "Google Sign-In failed. Please try again."
            } finally {
                isLoading = false
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.login_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Snackbar host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(80.dp)) // Push below logo area

            // Echo App Name************************************************************************
            Text(
                text = "Echo",
                style = TextStyle(
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyBlue,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            //Tagline*******************************************************************************
            Text(
                text = "Stay Connected, Locally.",
                fontSize = 18.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Email Input**************************************************************************
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Input***********************************************************************
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible) {
                        Icons.Filled.VisibilityOff
                    } else {
                        Icons.Filled.Visibility
                    }
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = null)
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Email/Password Login Button**********************************************************
            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        try {
                            authViewModel.signInWithEmail(email, password)
                        } catch (e: Exception) {
                            errorMessage = "Sign-In failed. Please check your credentials."
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavyBlue,
                    contentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Login")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            //Forgot Password/Sign Up Button********************************************************
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.85f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        // TODO: Navigate to Forgot Password
                    }
                ) {
                    Text(
                        "Forgot Password?",
                        color = Color.White,
                        textDecoration = TextDecoration.Underline)
                }

                TextButton(
                    onClick = {
                        navController.navigate(Destinations.SIGN_UP)
                    }
                ) {
                    Text(
                        text = "Sign Up",
                        color = Color.White,
                        textDecoration = TextDecoration.Underline
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Divider with "or"********************************************************************
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.85f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White)
                Text(
                    text = "  OR  ",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Divider(modifier = Modifier.weight(1f), color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Google Sign-In Button****************************************************************
            GoogleSignInButton {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Continue as Guest********************************************************************
            TextButton(
                onClick = {
                    authViewModel.signInAsGuest()
                }
            ) {
                Text("Continue as Guest", color = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Terms and Privacy Policy Text (at the very bottom)***********************************
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
