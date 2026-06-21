package dev.echoapp.echo.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * Hilt module providing Coroutine Dispatchers.
 * Using qualifiers allows easy swapping in tests.
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
    
    @IoDispatcher
    @Provides
    @Singleton
    fun providesIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
    
    @DefaultDispatcher
    @Provides
    @Singleton
    fun providesDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
    
    @MainDispatcher
    @Provides
    @Singleton
    fun providesMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    /**
     * Application-lifetime scope for shared hot flows. A [SupervisorJob] keeps one
     * failed child from cancelling the others; it's never cancelled (lives as long
     * as the process), which is intentional for app-wide shared streams.
     */
    @ApplicationScope
    @Provides
    @Singleton
    fun providesApplicationScope(
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)
}
