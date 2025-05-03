package com.example.echo.ui.map

import com.example.echo.models.Post

sealed class MapUiState {
    object Loading : MapUiState()

    data class Success(
        val posts: List<Post>,
        val filteredPosts: List<Post>,
        val postLikes: Map<String, Int>,
        val userLikes: Set<String>,
        val commentCount: Map<String, Int>,
        val currentTag: String? = null,
        val isRefreshing: Boolean = false
    ) : MapUiState()

    data class Error(val message: String) : MapUiState()
}
