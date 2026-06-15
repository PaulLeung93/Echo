package com.example.echo.data.entity

/**
 * Firestore entity representing a Post document.
 * This class is used for Firestore serialization/deserialization.
 * All properties have default values for Firebase's toObject() method.
 */
data class PostEntity(
    val id: String = "",
    /** Stable Firebase Auth uid of the author. Used for non-spoofable ownership in security rules. */
    val authorId: String = "",
    val username: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** Standard base32 geohash of (latitude, longitude); enables viewport range queries on the map. */
    val geohash: String? = null,
    val tags: List<String> = emptyList(),
    val likeCount: Int? = null,
    val commentCount: Int? = null,
    val likes: List<String> = emptyList()
)
