package lk.moneytrail.app.domain.repository

import lk.moneytrail.app.domain.model.AuthUser

// Failures are returned as an lk.moneytrail.app.domain.model.AppException whose
// message can be shown to the user.
interface AuthRepository {

    /** The signed-in user's id, or null when nobody is signed in. */
    val currentUserId: String?

    val currentUserEmail: String?

    suspend fun signIn(email: String, password: String): Result<Unit>

    /** Creates the account, signs the user in and returns the new user's id. */
    suspend fun register(email: String, password: String): Result<String>

    /** Signs in with the ID token of the Google account the user picked. Makes the account on first use. */
    suspend fun signInWithGoogle(idToken: String): Result<AuthUser>

    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    /** Deletes the signed-in user's account. Used to undo a registration that only half finished. */
    suspend fun deleteCurrentUser(): Result<Unit>

    fun signOut()
}
