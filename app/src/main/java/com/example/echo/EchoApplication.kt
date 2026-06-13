package com.example.echo

import android.app.Application
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class required for Hilt dependency injection.
 */
@HiltAndroidApp
class EchoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // App Check attests that requests to Firebase (Auth, Firestore, and any
        // callable Cloud Functions) come from the genuine, unmodified app — the
        // first line of defense against someone scripting the backend directly.
        // NOTE: installing the provider is harmless until enforcement is turned
        // ON in the Firebase console (App Check > APIs). Until then tokens are
        // sent but not required, so nothing breaks.
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            if (BuildConfig.DEBUG) {
                // Debug builds can't pass Play Integrity (unsigned, on an
                // emulator), so they emit a debug token to logcat. Register it
                // under App Check > Apps > Manage debug tokens to test enforcement.
                DebugAppCheckProviderFactory.getInstance()
            } else {
                // Release builds: hardware-backed attestation via Play Integrity.
                PlayIntegrityAppCheckProviderFactory.getInstance()
            }
        )
    }
}
