package com.example.echo.ui.auth

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Track whether the user is signed in
    private val _isSignedIn = MutableStateFlow(auth.currentUser != null)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn

    /**
     * Handle Google Sign-In Result
     */
    fun handleGoogleSignInResult(result: androidx.activity.result.ActivityResult) {
        val data = result.data ?: return
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(data)
        val account = task.result ?: return

        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener { taskResult ->
            _isSignedIn.value = taskResult.isSuccessful
        }
    }

    /**
     * Email/Password Sign-In
     */
    fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password.trim())
            .addOnCompleteListener { task ->
                _isSignedIn.value = task.isSuccessful
            }
    }

    /**
     * Guest Sign-In (Anonymous)
     */
    fun signInAsGuest() {
        auth.signInAnonymously()
            .addOnCompleteListener { task ->
                _isSignedIn.value = task.isSuccessful
            }
    }

    /**
     * Sign Out
     */
    fun signOut() {
        auth.signOut()
        _isSignedIn.value = false
    }
}
