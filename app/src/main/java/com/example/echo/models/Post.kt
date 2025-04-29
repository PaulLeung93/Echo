package com.example.echo.models

data class Post(
    val id: String = "",
    val username: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val latitude: Double? = null,
    val longitude: Double? = null
)
