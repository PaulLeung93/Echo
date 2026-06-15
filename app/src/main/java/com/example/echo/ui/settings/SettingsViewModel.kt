package com.example.echo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echo.data.preferences.UserPreferencesRepository
import com.example.echo.domain.repository.AuthProvider
import com.example.echo.domain.repository.ReauthCredential
import com.example.echo.domain.repository.UserRepository
import com.example.echo.domain.usecase.user.DeleteAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: UserPreferencesRepository,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val userRepository: UserRepository
) : ViewModel() {

    /** Which re-auth flow the delete dialog should use for this account. */
    val authProvider: AuthProvider get() = userRepository.currentAuthProvider()

    val darkMode: StateFlow<Boolean> = preferences.darkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val notificationsEnabled: StateFlow<Boolean> = preferences.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { preferences.setDarkMode(enabled) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setNotificationsEnabled(enabled) }
    }

    /**
     * Deletes the account after re-authenticating with [reauth]. On success the
     * reactive auth state routes to Sign In; on failure nothing was deleted.
     */
    fun deleteAccount(reauth: ReauthCredential) {
        if (_isDeleting.value) return
        viewModelScope.launch {
            _isDeleting.value = true
            deleteAccountUseCase(reauth).onFailure { e ->
                _uiEvent.send(e.message ?: "Couldn't delete your account. Please try again.")
            }
            _isDeleting.value = false
        }
    }

    fun deleteAccountWithPassword(password: String) =
        deleteAccount(ReauthCredential.Password(password))

    fun deleteAccountWithGoogle(idToken: String) =
        deleteAccount(ReauthCredential.Google(idToken))
}
