package com.example.echo.di

import com.example.echo.data.repository.AuthRepositoryImpl
import com.example.echo.data.repository.CommentRepositoryImpl
import com.example.echo.data.repository.PoiRepositoryImpl
import com.example.echo.data.repository.PostRepositoryImpl
import com.example.echo.domain.repository.AuthRepository
import com.example.echo.domain.repository.CommentRepository
import com.example.echo.domain.repository.PoiRepository
import com.example.echo.domain.repository.PostRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds repository interfaces to their implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindPostRepository(impl: PostRepositoryImpl): PostRepository
    
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
    
    @Binds
    @Singleton
    abstract fun bindCommentRepository(impl: CommentRepositoryImpl): CommentRepository
    
    @Binds
    @Singleton
    abstract fun bindPoiRepository(impl: PoiRepositoryImpl): PoiRepository
}
