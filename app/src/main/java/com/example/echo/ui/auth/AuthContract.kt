package com.example.echo.ui.auth

import com.example.echo.domain.model.User

/**
 * UI State for the Authentication screens.
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentUser: User? = null,
    val isGoogleLoading: Boolean = false
)

/**
 * One-shot UI events for Authentication.
 */
sealed class AuthUiEvent {
    object NavigateToHome : AuthUiEvent()
    object NavigateToSignIn : AuthUiEvent()
    data class ShowError(val message: String) : AuthUiEvent()
    object SignInSuccess : AuthUiEvent()
}
