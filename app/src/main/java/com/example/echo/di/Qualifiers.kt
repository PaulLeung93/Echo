package com.example.echo.di

import javax.inject.Qualifier

/**
 * Qualifier for IO Dispatcher (disk/network operations).
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class IoDispatcher

/**
 * Qualifier for Default Dispatcher (CPU-intensive work).
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class DefaultDispatcher

/**
 * Qualifier for Main Dispatcher (UI operations).
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class MainDispatcher
