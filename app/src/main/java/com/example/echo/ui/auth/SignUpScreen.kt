package com.example.echo.ui.auth

import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.echo.R
import com.example.echo.navigation.Destinations
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material3.TextFieldDefaults



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(navController: NavHostController) {
    val auth = remember { FirebaseAuth.getInstance() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.signup_background),
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
            Text(text = "Create Account", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))


            // Email Input Field********************************************************************
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White),
                modifier = Modifier.fillMaxWidth()

            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Input Field*****************************************************************
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Password Input Field*********************************************************
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Sign Up Button***********************************************************************
            Button(
                onClick = {
                    // Input validation before calling Firebase
                    if (!isValidEmail(email)) {
                        errorMessage = "Please enter a valid email address."
                        return@Button
                    }
                    if (password.isBlank() || confirmPassword.isBlank()) {
                        errorMessage = "Password fields cannot be empty."
                        return@Button
                    }
                    if (password != confirmPassword) {
                        errorMessage = "Passwords do not match."
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null

                    // Create account with Firebase
                    auth.createUserWithEmailAndPassword(email.trim(), password.trim())
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                // Navigate back to login screen with a success message
                                navController.navigate("${Destinations.SIGN_IN}?successMessage=Account created successfully! Please log in.") {
                                    popUpTo(Destinations.SIGN_IN) { inclusive = true }
                                }
                            } else {
                                errorMessage = mapFirebaseErrorMessage(task.exception?.localizedMessage)
                            }
                        }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(text = "Sign Up")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigate back to login if already have account***************************************
            TextButton(
                onClick = { navController.popBackStack() }
            ) {
                Text(text = "Already have an account? Sign In")
            }

            // Show error message if sign up fails
            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

fun mapFirebaseErrorMessage(rawMessage: String?): String {
    return when {
        rawMessage?.contains("password is invalid", ignoreCase = true) == true -> "Incorrect password. Please try again."
        rawMessage?.contains("no user record", ignoreCase = true) == true -> "No account found with that email."
        rawMessage?.contains("email address is badly formatted", ignoreCase = true) == true -> "Invalid email address format."
        rawMessage?.contains("already in use", ignoreCase = true) == true -> "An account with this email already exists."
        else -> "Authentication failed. Please try again."
    }
}

fun isValidEmail(email: String): Boolean {
    return Patterns.EMAIL_ADDRESS.matcher(email).matches()
}
