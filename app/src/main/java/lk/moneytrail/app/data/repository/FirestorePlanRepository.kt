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
import lk.moneytrail.app.domain.repository.ConnectivityRepository
import lk.moneytrail.app.domain.repository.PlanRepository
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The money plan is one document, users/{uid}/settings/plan, with the goals as maps
 * inside it: everything a screen needs about the plan comes back in a single read.
 */
@Singleton
class FirestorePlanRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val connectivity: ConnectivityRepository
) : PlanRepository {

    override suspend fun savePlan(plan: MoneyPlan): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(AppException(SIGNED_OUT_MESSAGE))

        return try {
            planOf(uid).set(plan.toFirestore()).awaitServerBriefly(TAG, connectivity.isOnlineNow())
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

    private fun MoneyPlan.toFirestore(): Map<String, Any> = buildMap {
        put(FIELD_PAY_DAY, payDay)
        put(FIELD_BASE_SALARY, baseSalary)
        put(FIELD_INCOME_SOURCES, incomeSources.sortedBy { it.ordinal }.map { it.name })
        put(FIELD_GOALS, goals.mapIndexed { index, goal -> goal.toFirestore(index) })
        // The first goal again, for plans and screens from before there were several.
        put(FIELD_GOAL, goal.toFirestore(0))
        // Only written once they have a value, as the rules expect.
        savingsMovedMonth?.let { put(FIELD_SAVINGS_MOVED_MONTH, it.toString()) }
        savingsRemindOn?.let { put(FIELD_SAVINGS_REMIND_ON, it.toString()) }
        put(FIELD_UPDATED_AT, FieldValue.serverTimestamp())
    }

    private fun SavingsGoal.toFirestore(index: Int): Map<String, Any> = mapOf(
        GOAL_ID to id.ifBlank { legacyGoalId(index) },
        GOAL_NAME to name,
        GOAL_TARGET_AMOUNT to targetAmount,
        GOAL_SAVED_AMOUNT to savedAmount,
        GOAL_TARGET_DATE to targetDate.toString(),
        GOAL_TRANSFER_DAY to transferDay,
        GOAL_ALLOW_EMERGENCY_WITHDRAWALS to allowEmergencyWithdrawals
    )

    /** The id for a goal saved before goals had one: its place in the list, which stays put until it's saved again. */
    private fun legacyGoalId(index: Int) = "goal-$index"

    /** Null for a document with missing or unreadable fields, as if there were no plan. */
    private fun DocumentSnapshot.toPlan(): MoneyPlan? {
        val payDay = getLong(FIELD_PAY_DAY)?.toInt()
        val baseSalary = getLong(FIELD_BASE_SALARY)
        val sources = (get(FIELD_INCOME_SOURCES) as? List<*>).orEmpty()
            .mapNotNull { name -> IncomeSource.entries.find { it.name == name } }
            .toSet()

        // A goal saved without a transfer day is moved on pay day, like onboarding's.
        val defaultTransferDay = payDay ?: 1
        val goalsList = (get(FIELD_GOALS) as? List<*>)
            .orEmpty()
            .mapIndexedNotNull { index, goal -> (goal as? Map<*, *>)?.toGoal(index, defaultTransferDay) }
            .ifEmpty {
                (get(FIELD_GOAL) as? Map<*, *>)?.toGoal(0, defaultTransferDay)?.let { listOf(it) }.orEmpty()
            }

        if (payDay == null || baseSalary == null || goalsList.isEmpty()) {
            Log.w(TAG, "Ignoring a plan with missing or invalid fields")
            return null
        }
        return MoneyPlan(
            payDay = payDay,
            baseSalary = baseSalary,
            incomeSources = sources,
            goals = goalsList,
            savingsMovedMonth = getString(FIELD_SAVINGS_MOVED_MONTH)?.let { runCatching { YearMonth.parse(it) }.getOrNull() },
            savingsRemindOn = getString(FIELD_SAVINGS_REMIND_ON)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        )
    }

    private fun Map<*, *>.toGoal(index: Int, defaultTransferDay: Int): SavingsGoal? {
        val name = get(GOAL_NAME) as? String
        val target = (get(GOAL_TARGET_AMOUNT) as? Number)?.toLong()
        val saved = (get(GOAL_SAVED_AMOUNT) as? Number)?.toLong()
        val date = (get(GOAL_TARGET_DATE) as? String)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val transferDay = (get(GOAL_TRANSFER_DAY) as? Number)?.toInt() ?: defaultTransferDay
        val allowEmergency = (get(GOAL_ALLOW_EMERGENCY_WITHDRAWALS) as? Boolean) ?: true
        if (name == null || target == null || saved == null || date == null) return null
        return SavingsGoal(
            name = name,
            targetAmount = target,
            savedAmount = saved,
            targetDate = date,
            transferDay = transferDay,
            allowEmergencyWithdrawals = allowEmergency,
            id = (get(GOAL_ID) as? String)?.takeIf { it.isNotBlank() } ?: legacyGoalId(index)
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
        const val FIELD_SAVINGS_MOVED_MONTH = "savingsMovedMonth"
        const val FIELD_SAVINGS_REMIND_ON = "savingsRemindOn"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val GOAL_ID = "id"
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
