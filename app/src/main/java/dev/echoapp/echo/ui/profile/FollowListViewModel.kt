package dev.echoapp.echo.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.echoapp.echo.domain.usecase.follow.FollowListType
import dev.echoapp.echo.domain.usecase.follow.GetFollowListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FollowListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getFollowListUseCase: GetFollowListUseCase
) : ViewModel() {

    private val uid: String = savedStateHandle.get<String>("uid").orEmpty()
    private val initialType: FollowListType =
        if (savedStateHandle.get<String>("type") == "following") {
            FollowListType.FOLLOWING
        } else {
            FollowListType.FOLLOWERS
        }

    private val _selectedType = MutableStateFlow(initialType)

    val uiState: StateFlow<FollowListUiState> = _selectedType.flatMapLatest { type ->
        flow {
            emit(FollowListUiState(selectedType = type, isLoading = true))
            val result = getFollowListUseCase(uid, type)
            emit(
                result.fold(
                    onSuccess = { FollowListUiState(selectedType = type, profiles = it) },
                    onFailure = { FollowListUiState(selectedType = type, error = it.message ?: "Couldn't load this list.") }
                )
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FollowListUiState(selectedType = initialType, isLoading = true)
    )

    fun selectType(type: FollowListType) {
        _selectedType.value = type
    }
}
