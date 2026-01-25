package com.example.echo.data.entity

/**
 * Firestore entity representing a Comment document.
 * Used for Firestore serialization/deserialization.
 */
data class CommentEntity(
    val id: String? = null,
    val username: String = "",
    val message: String = "",
    val timestamp: Long = 0L
)
