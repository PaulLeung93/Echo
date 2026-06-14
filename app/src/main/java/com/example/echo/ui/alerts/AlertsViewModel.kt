package com.example.echo.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echo.domain.usecase.auth.GetCurrentUserUseCase
import com.example.echo.domain.usecase.post.GetPostsByAuthorIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * A single alert: engagement (likes / comments) on one of the user's own posts.
 *
 * This is a pragmatic v1 derived entirely from existing post data — no separate
 * notifications backend. It can later be extended to per-event alerts ("Sarah
 * commented…") or proximity alerts ("new post near you") behind the same UI.
 */
data class Alert(
    val postId: String,
    val postSnippet: String,
    val likeCount: Int,
    val commentCount: Int,
    val timestamp: Long
)

data class AlertsUiState(
    val alerts: List<Alert> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AlertsViewModel @Inject constructor(
    getCurrentUserUseCase: GetCurrentUserUseCase,
    getPostsByAuthorIdUseCase: GetPostsByAuthorIdUseCase
) : ViewModel() {

    private val user = getCurrentUserUseCase()

    val uiState: StateFlow<AlertsUiState> = if (user != null) {
        getPostsByAuthorIdUseCase(user.id)
            .map { posts ->
                val alerts = posts
                    .filter { it.likeCount > 0 || it.commentCount > 0 }
                    .sortedByDescending { it.timestamp }
                    .map { post ->
                        Alert(
                            postId = post.id,
                            postSnippet = post.message,
                            likeCount = post.likeCount,
                            commentCount = post.commentCount,
                            timestamp = post.timestamp
                        )
                    }
                AlertsUiState(alerts = alerts, isLoading = false)
            }
            .catch { e -> emit(AlertsUiState(isLoading = false, error = e.message)) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = AlertsUiState(isLoading = true)
            )
    } else {
        MutableStateFlow(AlertsUiState(isLoading = false, error = "Sign in to see your alerts")).asStateFlow()
    }
}
