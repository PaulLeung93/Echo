package dev.echoapp.echo.domain.model

/**
 * Domain model representing a Point of Interest.
 * Pure Kotlin representation with no Firebase GeoPoint dependency.
 */
data class Poi(
    val id: String,
    val name: String,
    val type: String,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    /** Number of posts in this POI's thread (denormalized; maintained on create/delete). */
    val postCount: Int = 0,
    /** Optional curated photo URL for this place; null falls back to a type icon. */
    val imageUrl: String? = null
)
