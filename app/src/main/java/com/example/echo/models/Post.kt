package com.example.echo.models

data class Post(
    val id: String = "",
    val username: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val tags: List<String> = emptyList(),

    // Denormalized fields
    val likeCount: Int? = null,
    val commentCount: Int? = null,

    // Full list of user IDs who liked the post (needed for filtering in ViewModel)
    val likes: List<String> = emptyList()
)
