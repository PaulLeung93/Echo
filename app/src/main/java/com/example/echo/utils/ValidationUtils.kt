package com.example.echo.utils

import android.util.Patterns

// Helper functions
fun isValidEmail(email: String): Boolean {
    return Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

fun isStrongPassword(password: String): Boolean {
    val uppercase = Regex("[A-Z]")
    val lowercase = Regex("[a-z]")
    val digit = Regex("[0-9]")
    val special = Regex("[!@#\$%^&*(),.?\":{}|<>]")

    return password.contains(uppercase) &&
            password.contains(lowercase) &&
            password.contains(digit) &&
            password.contains(special)
}