package com.example.echo.domain.model

/**
 * Domain model representing the current authenticated user.
 * Provides authentication state without Firebase dependencies.
 */
data class User(
    val id: String,
    val email: String?,
    val isAnonymous: Boolean
)
