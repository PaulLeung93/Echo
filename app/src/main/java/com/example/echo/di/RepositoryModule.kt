package com.example.echo.di

import com.example.echo.data.location.LocationProviderImpl
import com.example.echo.data.repository.AuthorAvatarResolverImpl
import com.example.echo.data.repository.AuthRepositoryImpl
import com.example.echo.data.repository.CommentRepositoryImpl
import com.example.echo.data.repository.PoiRepositoryImpl
import com.example.echo.data.repository.PostRepositoryImpl
import com.example.echo.data.repository.ReportRepositoryImpl
import com.example.echo.data.repository.UserRepositoryImpl
import com.example.echo.domain.repository.AuthRepository
import com.example.echo.domain.repository.AuthorAvatarResolver
import com.example.echo.domain.repository.CommentRepository
import com.example.echo.domain.repository.LocationProvider
import com.example.echo.domain.repository.PoiRepository
import com.example.echo.domain.repository.PostRepository
import com.example.echo.domain.repository.ReportRepository
import com.example.echo.domain.repository.UserRepository
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

    @Binds
    @Singleton
    abstract fun bindLocationProvider(impl: LocationProviderImpl): LocationProvider

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindReportRepository(impl: ReportRepositoryImpl): ReportRepository

    @Binds
    @Singleton
    abstract fun bindAuthorAvatarResolver(impl: AuthorAvatarResolverImpl): AuthorAvatarResolver
}
