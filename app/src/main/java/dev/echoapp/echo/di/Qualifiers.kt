package dev.echoapp.echo.di

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

/**
 * Qualifier for an application-lifetime CoroutineScope. Used to host shared,
 * long-lived flows (e.g. the single posts snapshot listener) that should outlive
 * any one ViewModel so collectors can share one upstream instead of each opening
 * its own.
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class ApplicationScope
