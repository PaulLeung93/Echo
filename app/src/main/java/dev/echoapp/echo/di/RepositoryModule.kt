package dev.echoapp.echo.di

import dev.echoapp.echo.data.location.LocationProviderImpl
import dev.echoapp.echo.data.repository.AuthorAvatarResolverImpl
import dev.echoapp.echo.data.repository.AuthRepositoryImpl
import dev.echoapp.echo.data.repository.CommentRepositoryImpl
import dev.echoapp.echo.data.repository.PoiRepositoryImpl
import dev.echoapp.echo.data.repository.PostRepositoryImpl
import dev.echoapp.echo.data.repository.ReportRepositoryImpl
import dev.echoapp.echo.data.repository.UserRepositoryImpl
import dev.echoapp.echo.domain.repository.AuthRepository
import dev.echoapp.echo.domain.repository.AuthorAvatarResolver
import dev.echoapp.echo.domain.repository.CommentRepository
import dev.echoapp.echo.domain.repository.LocationProvider
import dev.echoapp.echo.domain.repository.PoiRepository
import dev.echoapp.echo.domain.repository.PostRepository
import dev.echoapp.echo.domain.repository.ReportRepository
import dev.echoapp.echo.domain.repository.UserRepository
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
