package dev.echoapp.echo.data.entity

import com.google.firebase.firestore.GeoPoint

/**
 * Firestore entity representing a Point of Interest document.
 * Uses Firebase's GeoPoint for location data.
 */
data class PoiEntity(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val location: GeoPoint = GeoPoint(0.0, 0.0),
    val description: String = "",
    val postCount: Int = 0,
    val imageUrl: String = "",
    /** Epoch millis of the most recent echo here; null if never posted to. */
    val lastPostAt: Long? = null
)
