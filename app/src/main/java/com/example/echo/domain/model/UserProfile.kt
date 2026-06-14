package com.example.echo.domain.model

/**
 * Domain model for a user's public profile (distinct from [User], which is the
 * Firebase auth session). Stored at `users/{uid}`.
 */
data class UserProfile(
    val uid: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val email: String
) {
    /** "First Last", trimmed (handles a missing last name gracefully). */
    val fullName: String get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
}
