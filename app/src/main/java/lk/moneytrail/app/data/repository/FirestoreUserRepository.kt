package lk.moneytrail.app.data.repository

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import lk.moneytrail.app.domain.model.AppException
import lk.moneytrail.app.domain.model.UserProfile
import lk.moneytrail.app.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreUserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {

    override suspend fun createProfile(profile: UserProfile): Result<Unit> = firestoreCall {
        // set() only completes once the server confirms the write, which never happens
        // while offline, so give up instead of leaving the user watching a spinner.
        withTimeout(WRITE_TIMEOUT_MS) {
            userDocument(profile.uid).set(
                mapOf(
                    FIELD_NAME to profile.name,
                    FIELD_EMAIL to profile.email,
                    FIELD_CREATED_AT to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Unit
    }

    override suspend fun getProfile(uid: String): Result<UserProfile?> = firestoreCall {
        val snapshot = userDocument(uid).get().await()
        if (snapshot.exists()) {
            UserProfile(
                uid = uid,
                name = snapshot.getString(FIELD_NAME).orEmpty(),
                email = snapshot.getString(FIELD_EMAIL).orEmpty(),
                createdAt = snapshot.getTimestamp(FIELD_CREATED_AT)?.toInstant()
            )
        } else {
            null
        }
    }

    private fun userDocument(uid: String): DocumentReference =
        firestore.collection(USERS_COLLECTION).document(uid)

    private suspend fun <T> firestoreCall(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Firestore call timed out", e)
            Result.failure(AppException(FIRESTORE_UNREACHABLE_MESSAGE, e))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Firestore call failed", e)
            Result.failure(AppException(e.toFirestoreMessage(PROFILE_FAILED_MESSAGE), e))
        }

    private companion object {
        const val TAG = "FirestoreUserRepository"
        const val USERS_COLLECTION = "users"
        const val FIELD_NAME = "name"
        const val FIELD_EMAIL = "email"
        const val FIELD_CREATED_AT = "createdAt"
        const val WRITE_TIMEOUT_MS = 15_000L
        const val PROFILE_FAILED_MESSAGE = "Couldn't load or save your profile. Please try again."
    }
}
