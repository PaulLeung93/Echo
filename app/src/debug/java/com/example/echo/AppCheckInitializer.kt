package com.example.echo

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug builds can't pass Play Integrity (unsigned, on an emulator), so they use
 * the debug provider, which prints a token to logcat. Register it under
 * App Check > Apps > Manage debug tokens to test enforcement.
 *
 * The matching release implementation lives in src/release.
 */
fun installAppCheck() {
    FirebaseAppCheck.getInstance()
        .installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
}
