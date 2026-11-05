package lk.moneytrail.app.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import lk.moneytrail.app.domain.model.AppException
import lk.moneytrail.app.domain.model.RecurringBill
import lk.moneytrail.app.domain.repository.ConnectivityRepository
import lk.moneytrail.app.domain.repository.RecurringBillRepository
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Recurring bills live in users/{uid}/recurringBills/{billId}. The month a bill was last
 * put on the calendar is stored as a yyyy-MM string, or null before the first time.
 */
@Singleton
class FirestoreRecurringBillRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val connectivity: ConnectivityRepository
) : RecurringBillRepository {

    override fun observeBills(): Flow<Result<List<RecurringBill>>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Result.failure(AppException(SIGNED_OUT_MESSAGE)))
            close()
            awaitClose()
            return@callbackFlow
        }

        val registration = billsOf(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Listening to recurring bills failed", error)
                trySend(Result.failure(AppException(error.toFirestoreMessage(LOAD_FAILED_MESSAGE), error)))
                close()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val bills = snapshot.documents
                    .mapNotNull { it.toBill() }
                    .sortedWith(compareBy({ it.dueDay }, { it.name.lowercase() }))
                trySend(Result.success(bills))
            }
        }
        awaitClose { registration.remove() }
    }

    override suspend fun saveBill(bill: RecurringBill): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(AppException(SIGNED_OUT_MESSAGE))

        return try {
            val doc = if (bill.id.isEmpty()) billsOf(uid).document() else billsOf(uid).document(bill.id)
            doc.set(bill.toFirestore()).awaitServerBriefly(TAG, connectivity.isOnlineNow())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Saving a recurring bill failed", e)
            Result.failure(AppException(e.toFirestoreMessage(SAVE_FAILED_MESSAGE), e))
        }
    }

    override suspend fun deleteBill(id: String): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(AppException(SIGNED_OUT_MESSAGE))

        return try {
            billsOf(uid).document(id).delete().awaitServerBriefly(TAG, connectivity.isOnlineNow())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Deleting recurring bill $id failed", e)
            Result.failure(AppException(e.toFirestoreMessage(DELETE_FAILED_MESSAGE), e))
        }
    }

    override suspend fun markCreated(id: String, month: YearMonth?): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(AppException(SIGNED_OUT_MESSAGE))

        return try {
            // update() leaves the rest of the bill as it is.
            billsOf(uid).document(id)
                .update(mapOf(FIELD_LAST_CREATED_MONTH to month?.toString(), FIELD_UPDATED_AT to FieldValue.serverTimestamp()))
                .awaitServerBriefly(TAG, connectivity.isOnlineNow())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Marking recurring bill $id failed", e)
            Result.failure(AppException(e.toFirestoreMessage(SAVE_FAILED_MESSAGE), e))
        }
    }

    private fun billsOf(uid: String): CollectionReference =
        firestore.collection(USERS_COLLECTION).document(uid).collection(BILLS_COLLECTION)

    private fun RecurringBill.toFirestore(): Map<String, Any?> = mapOf(
        FIELD_NAME to name,
        FIELD_AMOUNT to amount,
        FIELD_ACCOUNT to account,
        FIELD_CATEGORY to category,
        FIELD_DUE_DAY to dueDay,
        FIELD_IS_ACTIVE to isActive,
        FIELD_LAST_CREATED_MONTH to lastCreatedMonth?.toString(),
        FIELD_UPDATED_AT to FieldValue.serverTimestamp()
    )

    /** Null for a document with missing or unreadable fields, which is skipped rather than crashing the list. */
    private fun DocumentSnapshot.toBill(): RecurringBill? {
        val name = getString(FIELD_NAME)
        val amount = getLong(FIELD_AMOUNT)
        val account = getString(FIELD_ACCOUNT)
        val category = getString(FIELD_CATEGORY)
        val dueDay = getLong(FIELD_DUE_DAY)?.toInt()
        if (name == null || amount == null || account == null || category == null || dueDay == null) {
            Log.w(TAG, "Skipping recurring bill $id: missing or invalid fields")
            return null
        }
        return RecurringBill(
            id = id,
            name = name,
            amount = amount,
            account = account,
            category = category,
            dueDay = dueDay,
            isActive = getBoolean(FIELD_IS_ACTIVE) ?: true,
            lastCreatedMonth = getString(FIELD_LAST_CREATED_MONTH)?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
        )
    }

    private companion object {
        const val TAG = "FirestoreRecurringBillRepository"
        const val USERS_COLLECTION = "users"
        const val BILLS_COLLECTION = "recurringBills"
        const val FIELD_NAME = "name"
        const val FIELD_AMOUNT = "amount"
        const val FIELD_ACCOUNT = "account"
        const val FIELD_CATEGORY = "category"
        const val FIELD_DUE_DAY = "dueDay"
        const val FIELD_IS_ACTIVE = "isActive"
        const val FIELD_LAST_CREATED_MONTH = "lastCreatedMonth"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val LOAD_FAILED_MESSAGE = "Couldn't load your recurring bills. Please try again."
        const val SAVE_FAILED_MESSAGE = "Couldn't save the bill. Please try again."
        const val DELETE_FAILED_MESSAGE = "Couldn't delete the bill. Please try again."
    }
}
