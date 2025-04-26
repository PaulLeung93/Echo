package com.example.echo.ui.auth

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn

    init {
        // Check if user is already signed in
        _isSignedIn.value = auth.currentUser != null
    }

    fun handleGoogleSignInResult(result: ActivityResult) {
        viewModelScope.launch {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener { taskResult ->
                        if (taskResult.isSuccessful) {
                            Log.d("AuthViewModel", "Google sign-in successful")
                            _isSignedIn.value = true
                        } else {
                            Log.e("AuthViewModel", "Google sign-in failed", taskResult.exception)
                        }
                    }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google sign-in error", e)
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _isSignedIn.value = false
    }
}