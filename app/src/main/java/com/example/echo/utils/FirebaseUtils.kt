package com.example.echo.utils

fun mapFirebaseErrorMessage(rawMessage: String?): String {
    return when {
        rawMessage?.contains("password is invalid", ignoreCase = true) == true -> "Incorrect password. Please try again."
        rawMessage?.contains("no user record", ignoreCase = true) == true -> "No account found with that email."
        rawMessage?.contains("email address is badly formatted", ignoreCase = true) == true -> "Invalid email address format."
        rawMessage?.contains("already in use", ignoreCase = true) == true -> "An account with this email already exists."
        else -> "Authentication failed. Please try again."
    }
}