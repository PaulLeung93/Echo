package com.example.echo.ui.create

/**
 * UI State for the Create Post screen.
 */
data class CreatePostUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)
