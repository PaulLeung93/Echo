package com.example.echo.data.entity

/**
 * Firestore entity representing a Comment document.
 * Used for Firestore serialization/deserialization.
 */
data class CommentEntity(
    val id: String? = null,
    /** Stable Firebase Auth uid of the author. Used for non-spoofable ownership in security rules. */
    val authorId: String = "",
    val username: String = "",
    /** Author's avatar download URL, denormalized at create time (blank/absent = none). */
    val authorPhotoUrl: String? = null,
    val message: String = "",
    val timestamp: Long = 0L
)
