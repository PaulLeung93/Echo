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

/** Allowed username length range (inclusive). */
const val USERNAME_MIN_LENGTH = 3
const val USERNAME_MAX_LENGTH = 20

/**
 * A valid username is 3–20 chars, lowercase letters/digits/underscore only, and
 * must start with a letter or digit (no leading/trailing underscore). Usernames
 * are stored lowercased and are case-insensitively unique.
 */
fun isValidUsername(username: String): Boolean {
    return Regex("^[a-z0-9][a-z0-9_]{1,18}[a-z0-9]$").matches(username)
}