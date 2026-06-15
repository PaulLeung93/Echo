package com.example.echo.data.entity

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
    val commentCount: Int = 0,
    val imageUrl: String = ""
)
