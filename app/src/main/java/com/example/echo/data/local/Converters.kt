package com.example.echo.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room type converters for the small list fields cached with a post (e.g. tags).
 * Encoded as JSON so any tag content round-trips losslessly regardless of the
 * characters it contains.
 */
class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>): String = Json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else Json.decodeFromString(value)
}
