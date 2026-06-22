package dev.echoapp.echo.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.echoapp.echo.di.IoDispatcher
import dev.echoapp.echo.domain.usecase.user.BIO_MAX_LENGTH
import dev.echoapp.echo.domain.usecase.user.GetCurrentUserProfileUseCase
import dev.echoapp.echo.domain.usecase.user.RemoveAvatarUseCase
import dev.echoapp.echo.domain.usecase.user.UpdateAvatarUseCase
import dev.echoapp.echo.domain.usecase.user.UpdateUserProfileUseCase
import dev.echoapp.echo.utils.ImageUtils
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
    private val removeAvatarUseCase: RemoveAvatarUseCase,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    data class State(
        val firstName: String = "",
        val lastName: String = "",
        val bio: String = "",
        val username: String = "",
        val photoUrl: String? = null,
        // A photo the user picked but hasn't saved yet (committed in save()).
        val pendingAvatarUri: Uri? = null,
        // The user chose to remove their existing photo (committed in save()).
        val pendingRemoval: Boolean = false,
        val isLoading: Boolean = true,
        val isSaving: Boolean = false,
        val error: String? = null
    ) {
        val canSave: Boolean
            get() = firstName.isNotBlank() && lastName.isNotBlank() &&
                !isLoading && !isSaving

        /**
         * What the avatar should show right now: the staged pick first, then the
         * existing photo unless the user has staged a removal.
         */
        val displayPhotoUrl: String?
            get() = pendingAvatarUri?.toString() ?: if (pendingRemoval) null else photoUrl

        /** Whether there's a photo currently shown that "Remove" could clear. */
        val hasPhoto: Boolean
            get() = displayPhotoUrl != null
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

    /** Stage a picked image for preview; the upload happens only when the user saves. */
    fun onAvatarPicked(uri: Uri) =
        _state.update { it.copy(pendingAvatarUri = uri, pendingRemoval = false, error = null) }

    /** Stage removal of the current photo; committed when the user saves. */
    fun onRemovePhoto() =
        _state.update { it.copy(pendingAvatarUri = null, pendingRemoval = true, error = null) }

    fun onFirstNameChange(value: String) = _state.update { it.copy(firstName = value) }
    fun onLastNameChange(value: String) = _state.update { it.copy(lastName = value) }
    fun onBioChange(value: String) = _state.update { it.copy(bio = value.take(BIO_MAX_LENGTH)) }

    fun save(onSuccess: () -> Unit) {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }

            // Commit the photo change first (if any). Bail out before touching the
            // profile fields if it fails, so the avatar and text can't drift apart.
            val photoResult: Result<Unit> = when {
                s.pendingAvatarUri != null -> {
                    val bytes = withContext(ioDispatcher) {
                        ImageUtils.compressImageForUpload(context, s.pendingAvatarUri)
                    }
                    if (bytes == null) {
                        Result.failure(IllegalStateException("Couldn't read that image. Try another."))
                    } else {
                        updateAvatarUseCase(bytes).map { }
                    }
                }
                s.pendingRemoval -> removeAvatarUseCase()
                else -> Result.success(Unit)
            }

            photoResult.onFailure { e ->
                _state.update {
                    it.copy(isSaving = false, error = e.message ?: "Couldn't update your photo. Please try again.")
                }
                return@launch
            }

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
