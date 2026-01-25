package com.example.echo.data.mapper

import com.example.echo.domain.model.User
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject

/**
 * Mapper for converting Firebase user to domain User.
 */
class UserMapper @Inject constructor() {
    
    /**
     * Convert FirebaseUser to domain User.
     * @param firebaseUser The Firebase user, or null if not signed in.
     * @return The domain User, or null if no user provided.
     */
    fun toDomain(firebaseUser: FirebaseUser?): User? {
        if (firebaseUser == null) return null
        
        return User(
            id = firebaseUser.uid,
            email = firebaseUser.email,
            isAnonymous = firebaseUser.isAnonymous
        )
    }
}
