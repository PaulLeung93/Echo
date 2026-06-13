package com.example.echo.domain.model

/**
 * Domain model representing a Post.
 * This is the pure Kotlin representation used throughout the domain and presentation layers.
 * It has no Firebase or data layer dependencies.
 */
data class Post(
    val id: String,
    /** Stable Firebase Auth uid of the author (empty for legacy docs written before this field existed). */
    val authorId: String,
    val username: String,
    val message: String,
    val timestamp: Long,
    val latitude: Double?,
    val longitude: Double?,
    val tags: List<String>,
    val likeCount: Int,
    val commentCount: Int,
    val likedByCurrentUser: Boolean
)
