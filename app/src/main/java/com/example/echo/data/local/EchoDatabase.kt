package com.example.echo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Local Room database. Holds only disposable caches that mirror Firestore (currently
 * the feed), so schema changes drop-and-repopulate rather than ship migrations.
 */
// v2: added CachedPostEntity.authorPhotoUrl (destructive fallback recreates the cache).
@Database(entities = [CachedPostEntity::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class EchoDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
}
