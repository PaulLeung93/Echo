package com.example.echo.feature.map.presentation

import com.example.echo.domain.model.Comment
import com.example.echo.domain.model.Poi

data class PoiDetailUiState(
    val poi: Poi? = null,
    val comments: List<Comment> = emptyList(),
    val currentUserEmail: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
