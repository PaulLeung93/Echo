package dev.echoapp.echo.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.echoapp.echo.domain.usecase.auth.*
import dev.echoapp.echo.domain.usecase.user.GetCurrentUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val signInWithEmailUseCase: SignInWithEmailUseCase,
    private val signUpWithEmailUseCase: SignUpWithEmailUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val signInAsGuestUseCase: SignInAsGuestUseCase,
    private val deleteProvisionalAccountUseCase: DeleteProvisionalAccountUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val sendPasswordResetEmailUseCase: SendPasswordResetEmailUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<AuthUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        checkUserSession()
        // Keep currentUser in sync with Firebase reactively, so an expired/revoked
        // session (Firebase signs the user out) is noticed at runtime — RootNavHost
        // then routes back to Sign In.
        viewModelScope.launch {
            getCurrentUserUseCase.authState().collect { user ->
                _uiState.update { it.copy(currentUser = user) }
                // Decide whether this user still needs to set up a profile
                // (drives RootNavHost's cold-launch routing). Guests never do.
                if (user != null && !user.isAnonymous) {
                    _uiState.update { it.copy(needsProfileSetup = null) }
                    _uiState.update { it.copy(needsProfileSetup = resolveNeedsProfileSetup()) }
                } else {
                    _uiState.update { it.copy(needsProfileSetup = false) }
                }
            }
        }
    }

    /**
     * Whether the current non-anonymous user still needs to set up a profile.
     * Retries transient read errors so a failed read is never mistaken for
     * "no profile" (which would wrongly force an existing user through setup).
     * After a few tries, fails open to false — new accounts are still routed to
     * setup because their profile genuinely reads back as null.
     */
    private suspend fun resolveNeedsProfileSetup(): Boolean {
        repeat(3) { attempt ->
            val result = getCurrentUserProfileUseCase()
            if (result.isSuccess) return result.getOrNull() == null
            if (attempt < 2) delay(800)
        }
        return false
    }

    /** Called after the profile is created so routing reflects it immediately. */
    fun onProfileCompleted() {
        _uiState.update { it.copy(needsProfileSetup = false) }
    }

    fun checkUserSession() {
        val user = getCurrentUserUseCase()
        _uiState.update { it.copy(currentUser = user) }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            signInWithEmailUseCase(email, password)
                .onSuccess { user ->
                    // A signed-up user who abandoned profile setup must still
                    // finish it on sign-in — don't drop them straight into the app.
                    val needsSetup = resolveNeedsProfileSetup()
                    _uiState.update { it.copy(isLoading = false, currentUser = user, needsProfileSetup = needsSetup) }
                    if (needsSetup) {
                        _uiEvent.send(AuthUiEvent.NavigateToCompleteProfile)
                    } else {
                        _uiEvent.send(AuthUiEvent.SignInSuccess)
                        _uiEvent.send(AuthUiEvent.NavigateToHome)
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                    _uiEvent.send(AuthUiEvent.ShowError(error.message ?: "Sign in failed"))
                }
        }
    }

    fun signInAsGuest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            signInAsGuestUseCase()
                .onSuccess { user ->
                    _uiState.update { it.copy(isLoading = false, currentUser = user) }
                    _uiEvent.send(AuthUiEvent.NavigateToHome)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                    _uiEvent.send(AuthUiEvent.ShowError(error.message ?: "Guest sign in failed"))
                }
        }
    }

    fun signUpWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            signUpWithEmailUseCase(email, password)
                .onSuccess { user ->
                    // New account → must set up a profile before reaching the app.
                    _uiState.update { it.copy(isLoading = false, currentUser = user, needsProfileSetup = true) }
                    _uiEvent.send(AuthUiEvent.NavigateToCompleteProfile)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                    _uiEvent.send(AuthUiEvent.ShowError(error.message ?: "Sign up failed"))
                }
        }
    }

    /**
     * Abandon profile setup for a provisional account (the Complete Profile
     * "Cancel" escape hatch). Deletes the orphan account when possible, otherwise
     * signs out — either way the user ends up back at Sign In.
     */
    fun cancelProfileSetup() {
        viewModelScope.launch {
            deleteProvisionalAccountUseCase()
            _uiState.update { it.copy(currentUser = null, needsProfileSetup = false) }
            _uiEvent.send(AuthUiEvent.NavigateToSignIn)
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGoogleLoading = true, error = null) }
            signInWithGoogleUseCase(idToken)
                .onSuccess { user ->
                    // First-time Google users have no profile yet — route them to
                    // setup rather than straight into the app.
                    val needsSetup = resolveNeedsProfileSetup()
                    _uiState.update { it.copy(isGoogleLoading = false, currentUser = user, needsProfileSetup = needsSetup) }
                    if (needsSetup) {
                        _uiEvent.send(AuthUiEvent.NavigateToCompleteProfile)
                    } else {
                        _uiEvent.send(AuthUiEvent.NavigateToHome)
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isGoogleLoading = false, error = error.message) }
                    _uiEvent.send(AuthUiEvent.ShowError(error.message ?: "Google sign in failed"))
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            signOutUseCase()
            _uiState.update { it.copy(currentUser = null) }
            _uiEvent.send(AuthUiEvent.NavigateToSignIn)
        }
    }

    fun sendPasswordResetEmail(email: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            sendPasswordResetEmailUseCase(email)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                    _uiEvent.send(AuthUiEvent.ShowError(error.message ?: "Failed to send reset email"))
                }
        }
    }

    /**
     * Handle the result of the Google Sign-In activity.
     *
     * Distinguishes the three meaningful outcomes:
     *  - user dismissed the chooser (back/tap-outside/explicit cancel) → silent, no error
     *  - a real failure (network, misconfig, etc.) → a clean, mapped message
     *  - success with an ID token → hand off to [signInWithGoogle]
     *
     * Note: In a full refactor, the Google Sign-In logic should be moved to the Repository.
     */
    fun handleGoogleSignInResult(result: androidx.activity.result.ActivityResult) {
        // Back-press / tap-outside returns RESULT_CANCELED — not worth a snackbar.
        if (result.resultCode != android.app.Activity.RESULT_OK) return

        val task = com.google.android.gms.auth.api.signin.GoogleSignIn
            .getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                signInWithGoogle(idToken)
            } else {
                viewModelScope.launch {
                    _uiEvent.send(AuthUiEvent.ShowError("Couldn't read your Google account. Please try again."))
                }
            }
        } catch (e: com.google.android.gms.common.api.ApiException) {
            when (e.statusCode) {
                // User cancelled — silently ignore.
                com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.SIGN_IN_CANCELLED,
                com.google.android.gms.common.api.CommonStatusCodes.CANCELED -> Unit
                else -> viewModelScope.launch {
                    val message =
                        if (e.statusCode == com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.NETWORK_ERROR)
                            "Network error. Check your connection and try again."
                        else "Google sign in failed. Please try again."
                    _uiEvent.send(AuthUiEvent.ShowError(message))
                }
            }
        }
    }
}
