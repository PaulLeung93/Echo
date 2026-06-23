package dev.echoapp.echo.utils

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseUtilsTest {

    @Test
    fun `collision exception explains an account already exists`() {
        // e.g. Google sign-in for an email already registered with email/password.
        val e = mockk<FirebaseAuthUserCollisionException>(relaxed = true)
        assertTrue(mapFirebaseErrorMessage(e).contains("already exists", ignoreCase = true))
    }

    @Test
    fun `network exception explains a connection problem`() {
        val e = mockk<FirebaseNetworkException>(relaxed = true)
        assertEquals(
            "Network error. Check your connection and try again.",
            mapFirebaseErrorMessage(e)
        )
    }

    @Test
    fun `too-many-requests asks the user to wait`() {
        val e = mockk<FirebaseTooManyRequestsException>(relaxed = true)
        assertTrue(mapFirebaseErrorMessage(e).contains("Too many", ignoreCase = true))
    }

    @Test
    fun `weak password explains the minimum length`() {
        // FirebaseAuthWeakPasswordException is a subtype of the invalid-credentials
        // exception, so this also guards the type-check ordering in the mapper.
        val e = mockk<FirebaseAuthWeakPasswordException>(relaxed = true)
        assertTrue(mapFirebaseErrorMessage(e).contains("weak", ignoreCase = true))
    }

    @Test
    fun `invalid-email error code maps to a format message`() {
        val e = mockk<FirebaseAuthInvalidCredentialsException>(relaxed = true)
        every { e.errorCode } returns "ERROR_INVALID_EMAIL"
        assertEquals("Invalid email address format.", mapFirebaseErrorMessage(e))
    }

    @Test
    fun `other invalid credentials map to an ambiguous incorrect message`() {
        // Modern Firebase doesn't reveal whether the email or the password was wrong.
        val e = mockk<FirebaseAuthInvalidCredentialsException>(relaxed = true)
        every { e.errorCode } returns "ERROR_INVALID_CREDENTIAL"
        assertEquals("Incorrect email or password. Please try again.", mapFirebaseErrorMessage(e))
    }

    @Test
    fun `invalid user maps to no-account-found`() {
        val e = mockk<FirebaseAuthInvalidUserException>(relaxed = true)
        assertEquals("No account found with that email.", mapFirebaseErrorMessage(e))
    }

    @Test
    fun `unknown exception falls back to the generic message`() {
        assertEquals(
            "Authentication failed. Please try again.",
            mapFirebaseErrorMessage(RuntimeException("boom"))
        )
    }

    @Test
    fun `null error falls back to the generic message`() {
        assertEquals("Authentication failed. Please try again.", mapFirebaseErrorMessage(null))
    }

    @Test
    fun `raw-message fallback still recognizes legacy text`() {
        // For untyped exceptions we still scan the message for known phrases.
        assertEquals(
            "An account with this email already exists.",
            mapFirebaseErrorMessage(RuntimeException("The email address is already in use by another account."))
        )
    }
}
