package lk.moneytrail.app.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import lk.moneytrail.app.domain.model.AppException
import lk.moneytrail.app.domain.model.MonthClose
import lk.moneytrail.app.domain.repository.ConnectivityRepository
import lk.moneytrail.app.domain.repository.MonthCloseRepository
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A closed month lives in users/{uid}/closes/{yyyy-MM}: the month as the id keeps one
 * close per month, and closing it again simply replaces it.
 */
@Singleton
class FirestoreMonthCloseRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val connectivity: ConnectivityRepository
) : MonthCloseRepository {

    override fun observeClose(month: YearMonth): Flow<Result<MonthClose?>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Result.failure(AppException(SIGNED_OUT_MESSAGE)))
            close()
            awaitClose()
            return@callbackFlow
        }

        val registration = closeOf(uid, month).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Listening to the close of $month failed", error)
                trySend(Result.failure(AppException(error.toFirestoreMessage(LOAD_FAILED_MESSAGE), error)))
                close()
                return@addSnapshotListener
            }
            trySend(Result.success(snapshot?.takeIf { it.exists() }?.toClose(month)))
        }
        awaitClose { registration.remove() }
    }

    override suspend fun saveClose(close: MonthClose): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(AppException(SIGNED_OUT_MESSAGE))
        return try {
            closeOf(uid, close.month).set(
                mapOf(
                    FIELD_MONTH to close.month.toString(),
                    FIELD_BALANCES to close.balances,
                    FIELD_CLOSED_ON to close.closedOn.toString()
                )
            ).awaitServerBriefly(TAG, connectivity.isOnlineNow())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Saving the close of ${close.month} failed", e)
            Result.failure(AppException(e.toFirestoreMessage(SAVE_FAILED_MESSAGE), e))
        }
    }

    private fun closeOf(uid: String, month: YearMonth): DocumentReference =
        firestore.collection(USERS_COLLECTION).document(uid).collection(CLOSES_COLLECTION).document(month.toString())

    /** Null for a document with unreadable fields, which is treated as not closed. */
    private fun DocumentSnapshot.toClose(month: YearMonth): MonthClose? {
        val closedOn = getString(FIELD_CLOSED_ON)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val balances = (get(FIELD_BALANCES) as? Map<*, *>)
            ?.mapNotNull { (key, value) -> (key as? String)?.let { k -> (value as? Number)?.let { k to it.toLong() } } }
            ?.toMap()
        if (closedOn == null || balances == null) {
            Log.w(TAG, "Skipping close $id: missing or invalid fields")
            return null
        }
        return MonthClose(month, balances, closedOn)
    }

    private companion object {
        const val TAG = "FirestoreMonthClose"
        const val USERS_COLLECTION = "users"
        const val CLOSES_COLLECTION = "closes"
        const val FIELD_MONTH = "month"
        const val FIELD_BALANCES = "balances"
        const val FIELD_CLOSED_ON = "closedOn"
        const val LOAD_FAILED_MESSAGE = "Couldn't load the monthly close. Please try again."
        const val SAVE_FAILED_MESSAGE = "Couldn't save the monthly close. Please try again."
    }
}
