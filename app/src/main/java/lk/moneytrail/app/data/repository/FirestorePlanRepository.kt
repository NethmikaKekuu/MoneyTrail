package lk.moneytrail.app.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import lk.moneytrail.app.domain.model.AppException
import lk.moneytrail.app.domain.model.IncomeSource
import lk.moneytrail.app.domain.model.MoneyPlan
import lk.moneytrail.app.domain.model.SavingsGoal
import lk.moneytrail.app.domain.repository.PlanRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The money plan is one document, users/{uid}/settings/plan, with the goal as a map
 * inside it: everything a screen needs about the plan comes back in a single read.
 */
@Singleton
class FirestorePlanRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : PlanRepository {

    override suspend fun savePlan(plan: MoneyPlan): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(AppException(SIGNED_OUT_MESSAGE))

        return try {
            planOf(uid).set(plan.toFirestore()).awaitServerBriefly(TAG)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Saving the plan failed", e)
            Result.failure(AppException(e.toFirestoreMessage(SAVE_FAILED_MESSAGE), e))
        }
    }

    override suspend fun getPlan(): Result<MoneyPlan?> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(AppException(SIGNED_OUT_MESSAGE))

        return try {
            // Answered from the offline cache when there's no internet.
            val snapshot = planOf(uid).get().await()
            Result.success(if (snapshot.exists()) snapshot.toPlan() else null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Loading the plan failed", e)
            Result.failure(AppException(e.toFirestoreMessage(LOAD_FAILED_MESSAGE), e))
        }
    }

    override fun observePlan(): Flow<Result<MoneyPlan?>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Result.failure(AppException(SIGNED_OUT_MESSAGE)))
            close()
            awaitClose()
            return@callbackFlow
        }

        val registration = planOf(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Listening to plan failed", error)
                trySend(Result.failure(AppException(error.toFirestoreMessage(LOAD_FAILED_MESSAGE), error)))
                close()
                return@addSnapshotListener
            }
            val plan = snapshot?.takeIf { it.exists() }?.toPlan()
            trySend(Result.success(plan))
        }
        awaitClose { registration.remove() }
    }

    private fun planOf(uid: String): DocumentReference =
        firestore.collection(USERS_COLLECTION).document(uid).collection(SETTINGS_COLLECTION).document(PLAN_DOCUMENT)

    private fun MoneyPlan.toFirestore(): Map<String, Any> = mapOf(
        FIELD_PAY_DAY to payDay,
        FIELD_BASE_SALARY to baseSalary,
        FIELD_INCOME_SOURCES to incomeSources.sortedBy { it.ordinal }.map { it.name },
        FIELD_GOALS to goals.map { g ->
            mapOf(
                GOAL_NAME to g.name,
                GOAL_TARGET_AMOUNT to g.targetAmount,
                GOAL_SAVED_AMOUNT to g.savedAmount,
                GOAL_TARGET_DATE to g.targetDate.toString(),
                GOAL_TRANSFER_DAY to g.transferDay,
                GOAL_ALLOW_EMERGENCY_WITHDRAWALS to g.allowEmergencyWithdrawals
            )
        },
        FIELD_GOAL to mapOf(
            GOAL_NAME to goal.name,
            GOAL_TARGET_AMOUNT to goal.targetAmount,
            GOAL_SAVED_AMOUNT to goal.savedAmount,
            GOAL_TARGET_DATE to goal.targetDate.toString(),
            GOAL_TRANSFER_DAY to goal.transferDay,
            GOAL_ALLOW_EMERGENCY_WITHDRAWALS to goal.allowEmergencyWithdrawals
        ),
        FIELD_UPDATED_AT to FieldValue.serverTimestamp()
    )

    /** Null for a document with missing or unreadable fields, as if there were no plan. */
    private fun DocumentSnapshot.toPlan(): MoneyPlan? {
        val payDay = getLong(FIELD_PAY_DAY)?.toInt()
        val baseSalary = getLong(FIELD_BASE_SALARY)
        val sources = (get(FIELD_INCOME_SOURCES) as? List<*>).orEmpty()
            .mapNotNull { name -> IncomeSource.entries.find { it.name == name } }
            .toSet()

        val goalsList = (get(FIELD_GOALS) as? List<*>)
            .orEmpty()
            .mapNotNull { (it as? Map<*, *>)?.toGoal() }
            .ifEmpty {
                (get(FIELD_GOAL) as? Map<*, *>)?.toGoal()?.let { listOf(it) }.orEmpty()
            }

        if (payDay == null || baseSalary == null || goalsList.isEmpty()) {
            Log.w(TAG, "Ignoring a plan with missing or invalid fields")
            return null
        }
        return MoneyPlan(payDay = payDay, baseSalary = baseSalary, incomeSources = sources, goals = goalsList)
    }

    private fun Map<*, *>.toGoal(): SavingsGoal? {
        val name = get(GOAL_NAME) as? String
        val target = (get(GOAL_TARGET_AMOUNT) as? Number)?.toLong()
        val saved = (get(GOAL_SAVED_AMOUNT) as? Number)?.toLong()
        val date = (get(GOAL_TARGET_DATE) as? String)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val transferDay = (get(GOAL_TRANSFER_DAY) as? Number)?.toInt() ?: 25
        val allowEmergency = (get(GOAL_ALLOW_EMERGENCY_WITHDRAWALS) as? Boolean) ?: true
        if (name == null || target == null || saved == null || date == null) return null
        return SavingsGoal(
            name = name,
            targetAmount = target,
            savedAmount = saved,
            targetDate = date,
            transferDay = transferDay,
            allowEmergencyWithdrawals = allowEmergency
        )
    }

    private companion object {
        const val TAG = "FirestorePlanRepository"
        const val USERS_COLLECTION = "users"
        const val SETTINGS_COLLECTION = "settings"
        const val PLAN_DOCUMENT = "plan"
        const val FIELD_PAY_DAY = "payDay"
        const val FIELD_BASE_SALARY = "baseSalary"
        const val FIELD_INCOME_SOURCES = "incomeSources"
        const val FIELD_GOAL = "goal"
        const val FIELD_GOALS = "goals"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val GOAL_NAME = "name"
        const val GOAL_TARGET_AMOUNT = "targetAmount"
        const val GOAL_SAVED_AMOUNT = "savedAmount"
        const val GOAL_TARGET_DATE = "targetDate"
        const val GOAL_TRANSFER_DAY = "transferDay"
        const val GOAL_ALLOW_EMERGENCY_WITHDRAWALS = "allowEmergencyWithdrawals"
        const val SAVE_FAILED_MESSAGE = "Couldn't save your plan. Please try again."
        const val LOAD_FAILED_MESSAGE = "Couldn't load your plan. Please try again."
    }
}
