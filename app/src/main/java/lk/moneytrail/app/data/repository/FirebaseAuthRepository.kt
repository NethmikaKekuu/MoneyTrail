package lk.moneytrail.app.data.repository

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import lk.moneytrail.app.domain.model.AppException
import lk.moneytrail.app.domain.model.AuthUser
import lk.moneytrail.app.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    override val currentUserId: String?
        get() = auth.currentUser?.uid

    override val currentUserEmail: String?
        get() = auth.currentUser?.email

    override suspend fun signIn(email: String, password: String): Result<Unit> = authCall {
        auth.signInWithEmailAndPassword(email, password).await()
        Unit
    }

    override suspend fun register(email: String, password: String): Result<String> = authCall {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        checkNotNull(result.user) { "Firebase returned no user after sign-up" }.uid
    }

    override suspend fun signInWithGoogle(idToken: String): Result<AuthUser> =
        authCall(SignInMethod.GOOGLE) {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val user = checkNotNull(auth.signInWithCredential(credential).await().user) {
                "Firebase returned no user after Google sign-in"
            }
            AuthUser(uid = user.uid, email = user.email, displayName = user.displayName)
        }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = authCall {
        try {
            auth.sendPasswordResetEmail(email).await()
        } catch (e: FirebaseAuthInvalidUserException) {
            // Only projects without email enumeration protection report unknown emails.
            // Treat it like a sent email so the screen doesn't reveal who has an account.
            Log.i(TAG, "Password reset asked for an unknown email")
        }
        Unit
    }

    override suspend fun deleteCurrentUser(): Result<Unit> = authCall {
        auth.currentUser?.delete()?.await()
        Unit
    }

    override fun signOut() {
        auth.signOut()
    }

    private suspend fun <T> authCall(
        method: SignInMethod = SignInMethod.EMAIL,
        block: suspend () -> T
    ): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Firebase auth call failed", e)
            Result.failure(AppException(e.toUserMessage(method), e))
        }

    private fun Exception.toUserMessage(method: SignInMethod): String = when (this) {
        // Must come before FirebaseAuthInvalidCredentialsException, which it extends.
        is FirebaseAuthWeakPasswordException ->
            "That password is too weak. Use at least 8 characters with a letter and a number."
        is FirebaseAuthUserCollisionException ->
            method.emailTakenMessage
        is FirebaseAuthInvalidUserException ->
            if (errorCode == "ERROR_USER_DISABLED") "This account has been disabled."
            else method.wrongCredentialsMessage
        is FirebaseAuthInvalidCredentialsException ->
            method.wrongCredentialsMessage
        is FirebaseNetworkException ->
            "No internet connection. Check your connection and try again."
        is FirebaseTooManyRequestsException ->
            "Too many attempts. Wait a few minutes and try again."
        else ->
            if (isProviderDisabled()) "${method.consoleName} sign-in isn't turned on in the Firebase console."
            else "Something went wrong. Please try again."
    }

    // Firebase reports a disabled sign-in provider in two ways, depending on
    // whether Authentication was ever set up for the project.
    private fun Exception.isProviderDisabled(): Boolean =
        (this is FirebaseAuthException && errorCode == "ERROR_OPERATION_NOT_ALLOWED") ||
            message.orEmpty().contains("CONFIGURATION_NOT_FOUND")

    /** The same Firebase errors need different wording for email and Google sign-in. */
    private enum class SignInMethod(
        val consoleName: String,
        val wrongCredentialsMessage: String,
        val emailTakenMessage: String
    ) {
        EMAIL(
            consoleName = "Email/Password",
            wrongCredentialsMessage = "Email or password is incorrect.",
            emailTakenMessage = "An account with this email already exists. Try logging in instead."
        ),
        GOOGLE(
            consoleName = "Google",
            wrongCredentialsMessage = "Google couldn't confirm your account. Please try again.",
            emailTakenMessage = "This email already has a MoneyTrail password. Log in with your email and password."
        )
    }

    private companion object {
        const val TAG = "FirebaseAuthRepository"
    }
}
