package dev.echoapp.echo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row mirroring a feed [dev.echoapp.echo.domain.model.Post]. Backs the
 * offline-first feed: this cache is the feed list's display source of truth, kept
 * fresh from Firestore when online so the feed paints instantly — and still works —
 * on a cold or offline launch instead of spinning on a network round-trip.
 */
@Entity(tableName = "cached_feed_posts")
data class CachedPostEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val username: String,
    val authorPhotoUrl: String?,
    val message: String,
    val timestamp: Long,
    val latitude: Double?,
    val longitude: Double?,
    val tags: List<String>,
    val likeCount: Int,
    val commentCount: Int,
    val likedByCurrentUser: Boolean
)
