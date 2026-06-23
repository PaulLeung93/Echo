package dev.echoapp.echo.utils

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

/**
 * Map a Firebase auth failure to a friendly, user-facing message.
 *
 * Dispatches on exception *type* first (stable across locales and SDK versions),
 * falling back to substring matching on the raw message only for cases the typed
 * exceptions don't cover.
 */
fun mapFirebaseErrorMessage(error: Throwable?): String = when (error) {
    is FirebaseAuthWeakPasswordException ->
        "Password is too weak. Use at least 6 characters."

    is FirebaseAuthUserCollisionException ->
        // e.g. Google sign-in for an email already registered with email/password.
        "An account already exists with this email. Try signing in with your password or the original method."

    is FirebaseAuthInvalidCredentialsException ->
        if (error.errorCode == "ERROR_INVALID_EMAIL") "Invalid email address format."
        // Modern Firebase no longer distinguishes wrong-password from no-such-user
        // (email-enumeration protection), so keep the message ambiguous.
        else "Incorrect email or password. Please try again."

    is FirebaseAuthInvalidUserException ->
        "No account found with that email."

    is FirebaseNetworkException ->
        "Network error. Check your connection and try again."

    is FirebaseTooManyRequestsException ->
        "Too many attempts. Please wait a moment and try again."

    else -> mapFromRawMessage(error?.localizedMessage)
}

private fun mapFromRawMessage(rawMessage: String?): String = when {
    rawMessage?.contains("password is invalid", ignoreCase = true) == true -> "Incorrect password. Please try again."
    rawMessage?.contains("no user record", ignoreCase = true) == true -> "No account found with that email."
    rawMessage?.contains("email address is badly formatted", ignoreCase = true) == true -> "Invalid email address format."
    rawMessage?.contains("already in use", ignoreCase = true) == true -> "An account with this email already exists."
    rawMessage?.contains("network error", ignoreCase = true) == true -> "Network error. Check your connection and try again."
    else -> "Authentication failed. Please try again."
}
