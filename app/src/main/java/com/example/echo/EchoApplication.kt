package com.example.echo

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class required for Hilt dependency injection.
 */
@HiltAndroidApp
class EchoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Crashlytics: report uncaught crashes in release builds only, so local
        // dev/debug crashes don't pollute the production dashboard.
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG

        // App Check attests that requests to Firebase come from the genuine,
        // unmodified app. The provider differs per build type (debug provider
        // locally, Play Integrity in release) and is supplied by the matching
        // src/debug and src/release source set, so the debug-only artifact never
        // leaks into the release build. Harmless until enforcement is turned ON
        // in the Firebase console.
        installAppCheck()
    }
}
