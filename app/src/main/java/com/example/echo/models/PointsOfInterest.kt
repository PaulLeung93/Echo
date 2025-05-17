package com.example.echo.models

import com.google.firebase.firestore.GeoPoint

data class POI(
    val name: String = "",
    val type: String = "", // e.g. "college", "park", "landmark"
    val location: GeoPoint = GeoPoint(0.0, 0.0),
    val description: String = ""
)

