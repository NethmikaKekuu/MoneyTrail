package lk.moneytrail.app.data.repository

import android.util.Log
import com.google.android.gms.tasks.Task
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
import lk.moneytrail.app.domain.model.CustomCategory
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.SpendingType
import lk.moneytrail.app.domain.repository.CategoryRepository
import lk.moneytrail.app.domain.repository.ConnectivityRepository
import javax.inject.Inject
import javax.inject.Singleton

/** The user's own categories live in users/{uid}/categories/{categoryId}. */
@Singleton
class FirestoreCategoryRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val connectivity: ConnectivityRepository
) : CategoryRepository {

    override fun observeCategories(): Flow<Result<List<CustomCategory>>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Result.failure(AppException(SIGNED_OUT_MESSAGE)))
            close()
            awaitClose()
            return@callbackFlow
        }

        val registration = categoriesOf(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Listening to categories failed", error)
                trySend(Result.failure(AppException(error.toFirestoreMessage(LOAD_FAILED_MESSAGE), error)))
                close()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val categories = snapshot.documents
                    .mapNotNull { it.toCategory() }
                    .sortedBy { it.name.lowercase() }
                trySend(Result.success(categories))
            }
        }
        awaitClose { registration.remove() }
    }

    override suspend fun addCategory(category: CustomCategory): Result<Unit> = write("Adding a category") { uid ->
        categoriesOf(uid).document().set(
            mapOf(
                FIELD_NAME to category.name,
                FIELD_TYPE to category.type.name,
                FIELD_SPENDING_TYPE to category.spendingType?.name,
                FIELD_CREATED_AT to FieldValue.serverTimestamp()
            )
        )
    }

    override suspend fun deleteCategory(id: String): Result<Unit> = write("Deleting category $id") { uid ->
        categoriesOf(uid).document(id).delete()
    }

    private suspend fun write(action: String, write: (uid: String) -> Task<Void>): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(AppException(SIGNED_OUT_MESSAGE))

        return try {
            write(uid).awaitServerBriefly(TAG, connectivity.isOnlineNow())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "$action failed", e)
            Result.failure(AppException(e.toFirestoreMessage(SAVE_FAILED_MESSAGE), e))
        }
    }

    private fun categoriesOf(uid: String): CollectionReference =
        firestore.collection(USERS_COLLECTION).document(uid).collection(CATEGORIES_COLLECTION)

    /** Null for a document with missing or unreadable fields, which is skipped. */
    private fun DocumentSnapshot.toCategory(): CustomCategory? {
        val name = getString(FIELD_NAME)
        val type = getString(FIELD_TYPE)?.let { value -> EntryType.entries.find { it.name == value } }
        if (name == null || type == null) {
            Log.w(TAG, "Skipping category $id: missing or invalid fields")
            return null
        }
        return CustomCategory(
            id = id,
            name = name,
            type = type,
            spendingType = getString(FIELD_SPENDING_TYPE)?.let { value -> SpendingType.entries.find { it.name == value } }
        )
    }

    private companion object {
        const val TAG = "FirestoreCategoryRepository"
        const val USERS_COLLECTION = "users"
        const val CATEGORIES_COLLECTION = "categories"
        const val FIELD_NAME = "name"
        const val FIELD_TYPE = "type"
        const val FIELD_SPENDING_TYPE = "spendingType"
        const val FIELD_CREATED_AT = "createdAt"
        const val LOAD_FAILED_MESSAGE = "Couldn't load your categories. Please try again."
        const val SAVE_FAILED_MESSAGE = "Couldn't save your categories. Please try again."
    }
}
