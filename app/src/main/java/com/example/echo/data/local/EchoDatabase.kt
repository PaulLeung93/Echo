package com.example.echo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Local Room database. Holds only disposable caches that mirror Firestore (currently
 * the feed), so schema changes drop-and-repopulate rather than ship migrations.
 */
@Database(entities = [CachedPostEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class EchoDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
}
