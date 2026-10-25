package lk.moneytrail.app.data.repository

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import lk.moneytrail.app.domain.model.AppException
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryStatus
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.SpendingType
import lk.moneytrail.app.domain.repository.EntryRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Entries live in users/{uid}/entries/{entryId}. The date is stored as a yyyy-MM-dd
 * string, which sorts the same way as the dates, so a month (or several) is a single
 * range query on one field and needs no composite index.
 */
@Singleton
class FirestoreEntryRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : EntryRepository {

    override fun observeBetween(from: LocalDate, to: LocalDate): Flow<Result<List<Entry>>> =
        observeQuery { entries ->
            entries
                .whereGreaterThanOrEqualTo(FIELD_DATE, from.toString())
                .whereLessThanOrEqualTo(FIELD_DATE, to.toString())
        }

    // One equality filter on one field, so it needs no composite index either. Older
    // entries have no status field and are left out, which is right: they're confirmed.
    override fun observeExpected(): Flow<Result<List<Entry>>> =
        observeQuery { entries -> entries.whereEqualTo(FIELD_STATUS, EntryStatus.EXPECTED.name) }

    /** Listens to the user's entries that [query] picks, sorted by date and then time added. */
    private fun observeQuery(query: (CollectionReference) -> Query): Flow<Result<List<Entry>>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Result.failure(AppException(SIGNED_OUT_MESSAGE)))
            close()
            awaitClose()
            return@callbackFlow
        }

        // A snapshot listener, so screens update the moment an entry is saved. It answers
        // from the offline cache first, so they also open without internet.
        val registration = query(entriesOf(uid))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listening to entries failed", error)
                    trySend(Result.failure(AppException(error.toFirestoreMessage(LOAD_FAILED_MESSAGE), error)))
                    close()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val entries = snapshot.documents
                        .sortedWith(
                            compareBy<DocumentSnapshot>(
                                { it.getString(FIELD_DATE) },
                                // Entries saved offline have no server time yet; use Firestore's estimate.
                                { it.getTimestamp(FIELD_CREATED_AT, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                            )
                        )
                        .mapNotNull { it.toEntry() }
                    trySend(Result.success(entries))
                }
            }
        awaitClose { registration.remove() }
    }

    override suspend fun getEntry(id: String): Result<Entry?> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(AppException(SIGNED_OUT_MESSAGE))

        return try {
            // Answered from the offline cache when there's no internet; any entry shown
            // on the calendar is already there.
            val snapshot = entriesOf(uid).document(id).get().await()
            Result.success(if (snapshot.exists()) snapshot.toEntry() else null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Loading entry $id failed", e)
            Result.failure(AppException(e.toFirestoreMessage(LOAD_ONE_FAILED_MESSAGE), e))
        }
    }

    override suspend fun addEntry(entry: Entry): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(AppException(SIGNED_OUT_MESSAGE))

        return try {
            entriesOf(uid).document().set(entry.toFirestore()).awaitServerBriefly(TAG)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Saving an entry failed", e)
            Result.failure(AppException(e.toFirestoreMessage(SAVE_FAILED_MESSAGE), e))
        }
    }

    override suspend fun updateEntry(entry: Entry): Result<Unit> = writeEntry("Updating entry ${entry.id}") { uid ->
        // update() leaves createdAt alone, which the rules require.
        entriesOf(uid).document(entry.id).update(entry.toFirestoreFields())
    }

    override suspend fun deleteEntry(id: String): Result<Unit> = writeEntry("Deleting entry $id") { uid ->
        entriesOf(uid).document(id).delete()
    }

    /** Runs a write on one of the user's entries and turns failures into messages. */
    private suspend fun writeEntry(
        action: String,
        write: (uid: String) -> Task<Void>
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(AppException(SIGNED_OUT_MESSAGE))

        return try {
            write(uid).awaitServerBriefly(TAG)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "$action failed", e)
            val message = if ((e as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.NOT_FOUND) {
                GONE_MESSAGE
            } else {
                e.toFirestoreMessage(SAVE_FAILED_MESSAGE)
            }
            Result.failure(AppException(message, e))
        }
    }

    private fun entriesOf(uid: String): CollectionReference =
        firestore.collection(USERS_COLLECTION).document(uid).collection(ENTRIES_COLLECTION)

    private fun Entry.toFirestore(): Map<String, Any?> =
        toFirestoreFields() + (FIELD_CREATED_AT to FieldValue.serverTimestamp())

    /** Every field except createdAt, which is only set when the entry is first saved. */
    private fun Entry.toFirestoreFields(): Map<String, Any?> = mapOf(
        FIELD_STATUS to status.name,
        FIELD_TYPE to type.name,
        FIELD_AMOUNT to amount,
        FIELD_CATEGORY to category,
        FIELD_ACCOUNT to account,
        FIELD_DATE to date.toString(),
        FIELD_NOTE to note,
        FIELD_SPENDING_TYPE to spendingType?.name,
        FIELD_BONUS to bonus
    )

    /** Null for a document with missing or unreadable fields, which is skipped rather than crashing the calendar. */
    private fun DocumentSnapshot.toEntry(): Entry? {
        val type = getString(FIELD_TYPE)?.let { name -> EntryType.entries.find { it.name == name } }
        val amount = getLong(FIELD_AMOUNT)
        val category = getString(FIELD_CATEGORY)
        val account = getString(FIELD_ACCOUNT)
        val date = getString(FIELD_DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (type == null || amount == null || category == null || account == null || date == null) {
            Log.w(TAG, "Skipping entry $id: missing or invalid fields")
            return null
        }
        return Entry(
            id = id,
            type = type,
            amount = amount,
            category = category,
            account = account,
            date = date,
            note = getString(FIELD_NOTE).orEmpty(),
            spendingType = getString(FIELD_SPENDING_TYPE)?.let { name ->
                SpendingType.entries.find { it.name == name }
            },
            // Entries saved before expected entries existed have no status: they're confirmed.
            status = getString(FIELD_STATUS)?.let { name -> EntryStatus.entries.find { it.name == name } }
                ?: EntryStatus.CONFIRMED,
            // Entries saved before salary bonuses existed have no bonus.
            bonus = getLong(FIELD_BONUS) ?: 0L
        )
    }

    private companion object {
        const val TAG = "FirestoreEntryRepository"
        const val USERS_COLLECTION = "users"
        const val ENTRIES_COLLECTION = "entries"
        const val FIELD_STATUS = "status"
        const val FIELD_TYPE = "type"
        const val FIELD_AMOUNT = "amount"
        const val FIELD_CATEGORY = "category"
        const val FIELD_ACCOUNT = "account"
        const val FIELD_DATE = "date"
        const val FIELD_NOTE = "note"
        const val FIELD_SPENDING_TYPE = "spendingType"
        const val FIELD_BONUS = "bonus"
        const val FIELD_CREATED_AT = "createdAt"
        const val LOAD_FAILED_MESSAGE = "Couldn't load your entries. Please try again."
        const val SAVE_FAILED_MESSAGE = "Couldn't save the entry. Please try again."
        const val LOAD_ONE_FAILED_MESSAGE = "Couldn't load this entry. Please try again."
        const val GONE_MESSAGE = "This entry has already been deleted."
    }
}
