package dev.echoapp.echo.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.echoapp.echo.domain.usecase.user.CheckUsernameAvailabilityUseCase
import dev.echoapp.echo.domain.usecase.user.CreateUserProfileUseCase
import dev.echoapp.echo.domain.usecase.user.UsernameStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompleteProfileViewModel @Inject constructor(
    private val checkUsernameAvailabilityUseCase: CheckUsernameAvailabilityUseCase,
    private val createUserProfileUseCase: CreateUserProfileUseCase
) : ViewModel() {

    data class State(
        val firstName: String = "",
        val lastName: String = "",
        val username: String = "",
        /** null when the username field is empty / not yet checked. */
        val usernameStatus: UsernameStatus? = null,
        val isCheckingUsername: Boolean = false,
        val isSubmitting: Boolean = false,
        val error: String? = null
    ) {
        val canSubmit: Boolean
            get() = firstName.isNotBlank() &&
                lastName.isNotBlank() &&
                usernameStatus == UsernameStatus.Available &&
                !isCheckingUsername &&
                !isSubmitting
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var usernameJob: Job? = null

    fun onFirstNameChange(value: String) = _state.update { it.copy(firstName = value) }
    fun onLastNameChange(value: String) = _state.update { it.copy(lastName = value) }

    fun onUsernameChange(value: String) {
        // Usernames are lowercase; cap length live to match the rules.
        val handle = value.lowercase().take(20)
        _state.update { it.copy(username = handle, usernameStatus = null) }
        usernameJob?.cancel()
        if (handle.isEmpty()) {
            _state.update { it.copy(isCheckingUsername = false) }
            return
        }
        usernameJob = viewModelScope.launch {
            delay(400) // debounce typing
            _state.update { it.copy(isCheckingUsername = true) }
            val status = checkUsernameAvailabilityUseCase(handle)
            _state.update { it.copy(isCheckingUsername = false, usernameStatus = status) }
        }
    }

    fun submit(onSuccess: () -> Unit) {
        val s = _state.value
        if (!s.canSubmit) return
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }
            createUserProfileUseCase(s.firstName, s.lastName, s.username)
                .onSuccess {
                    _state.update { it.copy(isSubmitting = false) }
                    onSuccess()
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            error = e.message ?: "Couldn't create your profile. Please try again."
                        )
                    }
                }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
