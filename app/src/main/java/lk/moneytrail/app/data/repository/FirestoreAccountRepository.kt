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
import lk.moneytrail.app.domain.model.Account
import lk.moneytrail.app.domain.model.AccountKind
import lk.moneytrail.app.domain.model.AccountTemplates
import lk.moneytrail.app.domain.model.AppException
import lk.moneytrail.app.domain.repository.AccountRepository
import lk.moneytrail.app.domain.repository.ConnectivityRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Accounts live in users/{uid}/accounts/{accountId}, with the account's slug as the id. */
@Singleton
class FirestoreAccountRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val connectivity: ConnectivityRepository
) : AccountRepository {

    override fun observeAccounts(): Flow<Result<List<Account>>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Result.failure(AppException(SIGNED_OUT_MESSAGE)))
            close()
            awaitClose()
            return@callbackFlow
        }

        val registration = accountsOf(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Listening to accounts failed", error)
                trySend(Result.failure(AppException(error.toFirestoreMessage(LOAD_FAILED_MESSAGE), error)))
                close()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val accounts = snapshot.documents
                    .mapNotNull { it.toAccount() }
                    .sortedBy { onboardingOrder(it.id) }
                trySend(Result.success(accounts))
            }
        }
        awaitClose { registration.remove() }
    }

    override suspend fun saveAccounts(accounts: List<Account>): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(AppException(SIGNED_OUT_MESSAGE))

        return try {
            // One batch, so either every account is saved or none is.
            val batch = firestore.batch()
            accounts.forEach { account ->
                batch.set(accountsOf(uid).document(account.id), account.toFirestore())
            }
            batch.commit().awaitServerBriefly(TAG, connectivity.isOnlineNow())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Saving accounts failed", e)
            Result.failure(AppException(e.toFirestoreMessage(SAVE_FAILED_MESSAGE), e))
        }
    }

    override suspend fun saveBalanceCheck(accountId: String, balance: Long, date: LocalDate): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(AppException(SIGNED_OUT_MESSAGE))

        return try {
            // update() leaves the rest of the account, and when it was made, as they are.
            accountsOf(uid).document(accountId)
                .update(mapOf(FIELD_CHECKED_BALANCE to balance, FIELD_CHECKED_ON to date.toString()))
                .awaitServerBriefly(TAG, connectivity.isOnlineNow())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Saving the balance of $accountId failed", e)
            Result.failure(AppException(e.toFirestoreMessage(BALANCE_FAILED_MESSAGE), e))
        }
    }

    private fun accountsOf(uid: String): CollectionReference =
        firestore.collection(USERS_COLLECTION).document(uid).collection(ACCOUNTS_COLLECTION)

    /** Accounts sort in the order onboarding lists them; any others go last. */
    private fun onboardingOrder(id: String): Int =
        AccountTemplates.all.indexOfFirst { it.id == id }.let { if (it < 0) Int.MAX_VALUE else it }

    private fun Account.toFirestore(): Map<String, Any> = mapOf(
        FIELD_NAME to name,
        FIELD_DESCRIPTION to description,
        FIELD_KIND to kind.name,
        FIELD_CREATED_AT to FieldValue.serverTimestamp()
    )

    /** Null for a document with missing or unreadable fields, which is skipped. */
    private fun DocumentSnapshot.toAccount(): Account? {
        val name = getString(FIELD_NAME)
        val kind = getString(FIELD_KIND)?.let { value -> AccountKind.entries.find { it.name == value } }
        if (name == null || kind == null) {
            Log.w(TAG, "Skipping account $id: missing or invalid fields")
            return null
        }
        return Account(
            id = id,
            name = name,
            description = getString(FIELD_DESCRIPTION).orEmpty(),
            kind = kind,
            checkedBalance = getLong(FIELD_CHECKED_BALANCE),
            checkedOn = getString(FIELD_CHECKED_ON)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        )
    }

    private companion object {
        const val TAG = "FirestoreAccountRepository"
        const val USERS_COLLECTION = "users"
        const val ACCOUNTS_COLLECTION = "accounts"
        const val FIELD_NAME = "name"
        const val FIELD_DESCRIPTION = "description"
        const val FIELD_KIND = "kind"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_CHECKED_BALANCE = "checkedBalance"
        const val FIELD_CHECKED_ON = "checkedOn"
        const val BALANCE_FAILED_MESSAGE = "Couldn't save the balance. Please try again."
        const val LOAD_FAILED_MESSAGE = "Couldn't load your accounts. Please try again."
        const val SAVE_FAILED_MESSAGE = "Couldn't save your accounts. Please try again."
    }
}
