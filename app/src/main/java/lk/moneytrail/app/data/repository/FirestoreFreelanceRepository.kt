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
import kotlinx.coroutines.tasks.await
import lk.moneytrail.app.domain.model.AppException
import lk.moneytrail.app.domain.model.Milestone
import lk.moneytrail.app.domain.model.MilestoneStatus
import lk.moneytrail.app.domain.model.Project
import lk.moneytrail.app.domain.repository.ConnectivityRepository
import lk.moneytrail.app.domain.repository.FreelanceRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Projects live in users/{uid}/projects/{projectId}.
 */
@Singleton
class FirestoreFreelanceRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val connectivityRepository: ConnectivityRepository
) : FreelanceRepository {

    override fun observeProjects(): Flow<Result<List<Project>>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Result.failure(AppException(SIGNED_OUT_MESSAGE)))
            close()
            awaitClose()
            return@callbackFlow
        }

        val registration = projectsOf(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Listening to projects failed", error)
                trySend(Result.failure(AppException(error.toFirestoreMessage(LOAD_FAILED_MESSAGE), error)))
                close()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val projects = snapshot.documents
                    .mapNotNull { it.toProject() }
                    .sortedBy { it.name.lowercase() }
                trySend(Result.success(projects))
            }
        }
        awaitClose { registration.remove() }
    }

    override suspend fun getProject(id: String): Result<Project?> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(AppException(SIGNED_OUT_MESSAGE))

        return try {
            val snapshot = projectsOf(uid).document(id).get().await()
            Result.success(if (snapshot.exists()) snapshot.toProject() else null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Loading project $id failed", e)
            Result.failure(AppException(e.toFirestoreMessage(LOAD_FAILED_MESSAGE), e))
        }
    }

    override suspend fun saveProject(project: Project): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(AppException(SIGNED_OUT_MESSAGE))

        return try {
            val docRef = if (project.id.isNotBlank()) {
                projectsOf(uid).document(project.id)
            } else {
                projectsOf(uid).document()
            }

            val isOnline = connectivityRepository.isOnlineNow()
            docRef.set(project.toFirestore()).awaitServerBriefly(TAG, isOnline)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Saving project failed", e)
            Result.failure(AppException(e.toFirestoreMessage(SAVE_FAILED_MESSAGE), e))
        }
    }

    override suspend fun deleteProject(id: String): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(AppException(SIGNED_OUT_MESSAGE))

        return try {
            val isOnline = connectivityRepository.isOnlineNow()
            projectsOf(uid).document(id).delete().awaitServerBriefly(TAG, isOnline)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Deleting project $id failed", e)
            Result.failure(AppException(e.toFirestoreMessage(SAVE_FAILED_MESSAGE), e))
        }
    }

    private fun projectsOf(uid: String): CollectionReference =
        firestore.collection(USERS_COLLECTION).document(uid).collection(PROJECTS_COLLECTION)

    private fun Project.toFirestore(): Map<String, Any?> = mapOf(
        FIELD_NAME to name,
        FIELD_CLIENT to client,
        FIELD_TOTAL_BUDGET to totalBudget,
        FIELD_MILESTONES to milestones.map { it.toFirestore() },
        FIELD_UPDATED_AT to FieldValue.serverTimestamp()
    )

    private fun Milestone.toFirestore(): Map<String, Any?> = mapOf(
        FIELD_MILESTONE_ID to id,
        FIELD_MILESTONE_TITLE to title,
        FIELD_MILESTONE_AMOUNT to amount,
        FIELD_MILESTONE_STATUS to status.value,
        FIELD_MILESTONE_DUE_TIMESTAMP to dueTimestamp
    )

    private fun DocumentSnapshot.toProject(): Project? {
        val name = getString(FIELD_NAME)
        val client = getString(FIELD_CLIENT).orEmpty()
        val totalBudget = getLong(FIELD_TOTAL_BUDGET) ?: 0L
        if (name == null) return null

        val milestonesList = (get(FIELD_MILESTONES) as? List<*>).orEmpty()
            .mapNotNull { (it as? Map<*, *>)?.toMilestone() }

        return Project(
            id = id,
            name = name,
            client = client,
            totalBudget = totalBudget,
            milestones = milestonesList
        )
    }

    private fun Map<*, *>.toMilestone(): Milestone? {
        val id = (get(FIELD_MILESTONE_ID) as? String).orEmpty()
        val title = (get(FIELD_MILESTONE_TITLE) as? String) ?: return null
        val amount = (get(FIELD_MILESTONE_AMOUNT) as? Number)?.toLong() ?: 0L
        val statusStr = get(FIELD_MILESTONE_STATUS) as? String
        val dueTimestamp = (get(FIELD_MILESTONE_DUE_TIMESTAMP) as? Number)?.toLong() ?: 0L

        return Milestone(
            id = id,
            title = title,
            amount = amount,
            status = MilestoneStatus.fromString(statusStr),
            dueTimestamp = dueTimestamp
        )
    }

    private companion object {
        const val TAG = "FirestoreFreelanceRepository"
        const val USERS_COLLECTION = "users"
        const val PROJECTS_COLLECTION = "projects"
        const val FIELD_NAME = "name"
        const val FIELD_CLIENT = "client"
        const val FIELD_TOTAL_BUDGET = "totalBudget"
        const val FIELD_MILESTONES = "milestones"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val FIELD_MILESTONE_ID = "id"
        const val FIELD_MILESTONE_TITLE = "title"
        const val FIELD_MILESTONE_AMOUNT = "amount"
        const val FIELD_MILESTONE_STATUS = "status"
        const val FIELD_MILESTONE_DUE_TIMESTAMP = "dueTimestamp"
        const val LOAD_FAILED_MESSAGE = "Couldn't load your projects. Please try again."
        const val SAVE_FAILED_MESSAGE = "Couldn't save project. Please try again."
    }
}
