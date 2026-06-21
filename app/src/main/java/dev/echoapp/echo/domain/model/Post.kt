package dev.echoapp.echo.domain.model

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
    /** Author's avatar URL, denormalized at create time; null falls back to initials. */
    val authorPhotoUrl: String? = null,
    val message: String,
    val timestamp: Long,
    val latitude: Double?,
    val longitude: Double?,
    val tags: List<String>,
    val likeCount: Int,
    val commentCount: Int,
    val likedByCurrentUser: Boolean,
    /** Id of the POI this post belongs to, or null for an ordinary feed post. */
    val poiId: String? = null,
    /** Denormalized POI name (copied at create time) so the feed badge needs no extra read. */
    val poiName: String? = null
)
