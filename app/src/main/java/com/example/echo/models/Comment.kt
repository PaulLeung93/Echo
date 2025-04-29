package com.example.echo.models

data class Comment(
    val id: String? = null,
    val username: String = "",
    val message: String = "",
    val timestamp: Long = 0L
)