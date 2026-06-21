package dev.echoapp.echo.di

import android.content.Context
import androidx.room.Room
import dev.echoapp.echo.data.local.EchoDatabase
import dev.echoapp.echo.data.local.PostDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing the local Room database and its DAOs.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideEchoDatabase(@ApplicationContext context: Context): EchoDatabase =
        Room.databaseBuilder(context, EchoDatabase::class.java, "echo.db")
            // The DB is a disposable cache mirroring Firestore; on a schema change just
            // drop it and let the next refresh repopulate rather than ship migrations.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun providePostDao(database: EchoDatabase): PostDao = database.postDao()
}
