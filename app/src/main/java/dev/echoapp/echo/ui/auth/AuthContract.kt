package dev.echoapp.echo.ui.auth

import dev.echoapp.echo.domain.model.User

/**
 * UI State for the Authentication screens.
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentUser: User? = null,
    val isGoogleLoading: Boolean = false,
    /**
     * Whether a non-anonymous user still needs to set up their profile
     * (username + name). `null` while the check is in flight; drives the
     * cold-launch routing in RootNavHost.
     */
    val needsProfileSetup: Boolean? = null
)

/**
 * One-shot UI events for Authentication.
 */
sealed class AuthUiEvent {
    object NavigateToHome : AuthUiEvent()
    object NavigateToCompleteProfile : AuthUiEvent()
    object NavigateToSignIn : AuthUiEvent()
    data class ShowError(val message: String) : AuthUiEvent()
    object SignInSuccess : AuthUiEvent()
}
