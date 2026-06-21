package com.example.echo.data.entity

/**
 * Firestore entity for a `users/{uid}` document. All properties default for
 * Firebase's toObject() deserialization. (`createdAt` is written with a server
 * timestamp but not read back into the domain model.)
 */
data class UserProfileEntity(
    val uid: String = "",
    val username: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val bio: String = "",
    /** Download URL of the user's avatar in Cloud Storage; blank when none set. */
    val photoUrl: String = "",
    val blockedUserIds: List<String> = emptyList()
)
