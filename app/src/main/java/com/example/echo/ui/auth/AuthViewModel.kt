package com.example.echo.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echo.domain.usecase.auth.*
import com.example.echo.domain.usecase.user.GetCurrentUserProfileUseCase
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
    private val fetchSignInMethodsUseCase: FetchSignInMethodsUseCase,
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
                    // Retry transient read errors so we never mistake a failed
                    // read for "no profile" (which would wrongly force an existing
                    // user through profile setup). After a few tries, fail open to
                    // the app — new users are still routed to setup via sign-up.
                    var attempts = 0
                    while (true) {
                        val result = getCurrentUserProfileUseCase()
                        if (result.isSuccess) {
                            _uiState.update { it.copy(needsProfileSetup = result.getOrNull() == null) }
                            break
                        }
                        attempts++
                        if (attempts >= 3) {
                            _uiState.update { it.copy(needsProfileSetup = false) }
                            break
                        }
                        delay(800)
                    }
                } else {
                    _uiState.update { it.copy(needsProfileSetup = false) }
                }
            }
        }
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
                    _uiState.update { it.copy(isLoading = false, currentUser = user) }
                    _uiEvent.send(AuthUiEvent.SignInSuccess)
                    _uiEvent.send(AuthUiEvent.NavigateToHome)
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

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGoogleLoading = true, error = null) }
            signInWithGoogleUseCase(idToken)
                .onSuccess { user ->
                    _uiState.update { it.copy(isGoogleLoading = false, currentUser = user) }
                    _uiEvent.send(AuthUiEvent.NavigateToHome)
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

    suspend fun fetchSignInMethods(email: String): List<String> {
        return fetchSignInMethodsUseCase(email)
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
     * Legacy support for Google Sign-In helper that doesn't use ID token directly.
     * Note: In a full refactor, the Google Sign-In logic should be moved to the Repository.
     */
    fun handleGoogleSignInResult(result: androidx.activity.result.ActivityResult) {
        val data = result.data ?: return
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.result
            val idToken = account?.idToken
            if (idToken != null) {
                signInWithGoogle(idToken)
            } else {
                viewModelScope.launch {
                    _uiEvent.send(AuthUiEvent.ShowError("Google ID Token not found"))
                }
            }
        } catch (e: Exception) {
            viewModelScope.launch {
                _uiEvent.send(AuthUiEvent.ShowError(e.message ?: "Google sign in failed"))
            }
        }
    }
}
