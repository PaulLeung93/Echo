package com.example.echo.domain.model

/**
 * Domain model representing a Comment on a post.
 * Pure Kotlin representation with no Firebase dependencies.
 */
data class Comment(
    val id: String,
    /** Stable Firebase Auth uid of the author (empty for legacy docs written before this field existed). */
    val authorId: String,
    val username: String,
    val message: String,
    val timestamp: Long
)
