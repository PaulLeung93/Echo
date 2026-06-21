package com.example.echo.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echo.di.IoDispatcher
import com.example.echo.domain.usecase.user.BIO_MAX_LENGTH
import com.example.echo.domain.usecase.user.GetCurrentUserProfileUseCase
import com.example.echo.domain.usecase.user.UpdateAvatarUseCase
import com.example.echo.domain.usecase.user.UpdateUserProfileUseCase
import com.example.echo.utils.ImageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val updateAvatarUseCase: UpdateAvatarUseCase,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    data class State(
        val firstName: String = "",
        val lastName: String = "",
        val bio: String = "",
        val username: String = "",
        val photoUrl: String? = null,
        val isLoading: Boolean = true,
        val isSaving: Boolean = false,
        val isUploadingPhoto: Boolean = false,
        val error: String? = null
    ) {
        val canSave: Boolean
            get() = firstName.isNotBlank() && lastName.isNotBlank() &&
                !isLoading && !isSaving && !isUploadingPhoto
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getCurrentUserProfileUseCase()
                .onSuccess { profile ->
                    if (profile != null) {
                        _state.update {
                            it.copy(
                                firstName = profile.firstName,
                                lastName = profile.lastName,
                                bio = profile.bio,
                                username = profile.username,
                                photoUrl = profile.photoUrl,
                                isLoading = false
                            )
                        }
                    } else {
                        _state.update { it.copy(isLoading = false, error = "Couldn't load your profile.") }
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "Couldn't load your profile.") }
                }
        }
    }

    /** Compress the picked image off the main thread, upload it, and persist the URL. */
    fun onAvatarPicked(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isUploadingPhoto = true, error = null) }
            val bytes = withContext(ioDispatcher) { ImageUtils.compressImageForUpload(context, uri) }
            if (bytes == null) {
                _state.update {
                    it.copy(isUploadingPhoto = false, error = "Couldn't read that image. Try another.")
                }
                return@launch
            }
            updateAvatarUseCase(bytes)
                .onSuccess { url -> _state.update { it.copy(isUploadingPhoto = false, photoUrl = url) } }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isUploadingPhoto = false,
                            error = e.message ?: "Couldn't update your photo. Please try again."
                        )
                    }
                }
        }
    }

    fun onFirstNameChange(value: String) = _state.update { it.copy(firstName = value) }
    fun onLastNameChange(value: String) = _state.update { it.copy(lastName = value) }
    fun onBioChange(value: String) = _state.update { it.copy(bio = value.take(BIO_MAX_LENGTH)) }

    fun save(onSuccess: () -> Unit) {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            updateUserProfileUseCase(s.firstName, s.lastName, s.bio)
                .onSuccess {
                    _state.update { it.copy(isSaving = false) }
                    onSuccess()
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isSaving = false, error = e.message ?: "Couldn't save your changes. Please try again.")
                    }
                }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
