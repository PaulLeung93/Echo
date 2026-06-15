package com.example.echo.data.mapper

import com.example.echo.data.entity.PostEntity
import com.example.echo.domain.model.Post
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import javax.inject.Inject

/**
 * Mapper for converting between PostEntity (data layer) and Post (domain layer).
 * Handles the transformation in both directions while managing nullable currentUserId.
 */
class PostMapper @Inject constructor() {
    
    /**
     * Convert PostEntity to domain Post.
     * @param entity The Firestore entity to convert.
     * @param currentUserId The current user's ID for determining like status.
     * @return The domain Post model.
     */
    fun toDomain(entity: PostEntity, currentUserId: String?): Post {
        return Post(
            id = entity.id,
            authorId = entity.authorId,
            username = entity.username,
            message = entity.message,
            timestamp = entity.timestamp,
            latitude = entity.latitude,
            longitude = entity.longitude,
            tags = entity.tags,
            likeCount = entity.likeCount ?: entity.likes.size,
            commentCount = entity.commentCount ?: 0,
            likedByCurrentUser = currentUserId != null && entity.likes.contains(currentUserId)
        )
    }
    
    /**
     * Convert a list of PostEntities to domain Posts.
     * @param entities The list of Firestore entities.
     * @param currentUserId The current user's ID for determining like status.
     * @return List of domain Post models.
     */
    fun toDomainList(entities: List<PostEntity>, currentUserId: String?): List<Post> {
        return entities.map { toDomain(it, currentUserId) }
    }
    
    /**
     * Create a map representation for Firestore from post creation parameters.
     * @param authorId The stable Firebase Auth uid of the author (used for ownership in security rules).
     * @param username The username of the post author.
     * @param message The post message content.
     * @param latitude Optional latitude coordinate.
     * @param longitude Optional longitude coordinate.
     * @param tags List of tags for the post.
     * @param postId The generated post ID.
     * @return Map representation for Firestore document.
     */
    fun toFirestoreMap(
        authorId: String,
        username: String,
        message: String,
        latitude: Double?,
        longitude: Double?,
        tags: List<String>,
        postId: String
    ): Map<String, Any?> {
        return buildMap {
            put("id", postId)
            put("authorId", authorId)
            put("username", username)
            put("message", message)
            put("timestamp", System.currentTimeMillis())
            put("tags", tags)
            put("likeCount", 0)
            put("commentCount", 0)
            put("likes", emptyList<String>())
            if (latitude != null && longitude != null) {
                put("latitude", latitude)
                put("longitude", longitude)
                // Geohash lets the map fetch only posts within the visible viewport
                // (server-side range query) instead of downloading the whole feed.
                put("geohash", GeoFireUtils.getGeoHashForLocation(GeoLocation(latitude, longitude)))
            }
        }
    }
}
