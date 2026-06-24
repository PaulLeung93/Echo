package dev.echoapp.echo.domain.model

/**
 * Domain model for a user's public profile (distinct from [User], which is the
 * Firebase auth session). Stored at `users/{uid}`.
 */
data class UserProfile(
    val uid: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val bio: String = "",
    /** Avatar image URL (Cloud Storage download URL); null when the user has none. */
    val photoUrl: String? = null,
    /** Uids this user has blocked; their posts/comments are hidden from this user. */
    val blockedUserIds: List<String> = emptyList(),
    /**
     * POIs this user has favorited: poiId -> favoritedAt (epoch millis). A favorite
     * lets them post to that POI's thread regardless of the proximity radius. Capped at
     * [dev.echoapp.echo.utils.Constants.MAX_FAVORITE_POIS] and each slot is held for
     * [dev.echoapp.echo.utils.Constants.FAVORITE_HOLD_MILLIS] before it can be removed.
     */
    val favorites: Map<String, Long> = emptyMap(),
    /** Number of users following this profile (denormalized; maintained server-side). */
    val followerCount: Int = 0,
    /** Number of users this profile follows (denormalized; maintained server-side). */
    val followingCount: Int = 0
) {
    /** "First Last", trimmed (handles a missing last name gracefully). */
    val fullName: String get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
}
