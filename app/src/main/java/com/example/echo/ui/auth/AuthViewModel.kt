package com.example.echo.ui.auth

import androidx.lifecycle.ViewModel
import com.example.echo.utils.mapFirebaseErrorMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

sealed class SignInResult {
    object Success : SignInResult()
    class Error(val message: String) : SignInResult()
}

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _isSignedIn = MutableStateFlow(auth.currentUser != null)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn

    /**
     * Sign In with Email/Password
     */
    suspend fun signInWithEmail(email: String, password: String): SignInResult {
        return try {
            val result = auth.signInWithEmailAndPassword(email.trim(), password.trim()).await()
            if (result.user != null) {
                _isSignedIn.value = true
                SignInResult.Success
            } else {
                SignInResult.Error("Authentication failed.")
            }
        } catch (e: Exception) {
            SignInResult.Error(e.message ?: "Authentication failed.")
        }
    }

    /**
     * Sign Up with Email/Password
     */
    suspend fun signUpWithEmail(email: String, password: String): SignInResult {
        return try {
            auth.createUserWithEmailAndPassword(email.trim(), password.trim()).await()
            SignInResult.Success
        } catch (e: Exception) {
            SignInResult.Error(mapFirebaseErrorMessage(e.localizedMessage))
        }
    }


    /**
     * Sign in as Guest (Anonymous)
     */
    fun signInAsGuest() {
        auth.signInAnonymously().addOnCompleteListener { task ->
            _isSignedIn.value = task.isSuccessful
        }
    }

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
     * Reset Password
     */
    fun resetPassword(email: String, onComplete: (Boolean, String?) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.localizedMessage)
                }
            }
    }

    /**
     * Sign Out
     */
    fun signOut() {
        auth.signOut()
        _isSignedIn.value = false
    }

    suspend fun fetchSignInMethods(email: String): List<String>? {
        return try {
            val result = auth.fetchSignInMethodsForEmail(email.trim()).await()
            result.signInMethods
        } catch (e: Exception) {
            null
        }
    }

}
