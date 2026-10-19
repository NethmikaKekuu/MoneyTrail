package lk.moneytrail.app.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import lk.moneytrail.app.domain.model.AppException
import lk.moneytrail.app.domain.model.MoneyPlan
import lk.moneytrail.app.domain.repository.PlanRepository
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

    private fun planOf(uid: String): DocumentReference =
        firestore.collection(USERS_COLLECTION).document(uid).collection(SETTINGS_COLLECTION).document(PLAN_DOCUMENT)

    private fun MoneyPlan.toFirestore(): Map<String, Any> = mapOf(
        FIELD_PAY_DAY to payDay,
        FIELD_BASE_SALARY to baseSalary,
        FIELD_INCOME_SOURCES to incomeSources.sortedBy { it.ordinal }.map { it.name },
        FIELD_GOAL to mapOf(
            GOAL_NAME to goal.name,
            GOAL_TARGET_AMOUNT to goal.targetAmount,
            GOAL_SAVED_AMOUNT to goal.savedAmount,
            GOAL_TARGET_DATE to goal.targetDate.toString()
        ),
        FIELD_UPDATED_AT to FieldValue.serverTimestamp()
    )

    private companion object {
        const val TAG = "FirestorePlanRepository"
        const val USERS_COLLECTION = "users"
        const val SETTINGS_COLLECTION = "settings"
        const val PLAN_DOCUMENT = "plan"
        const val FIELD_PAY_DAY = "payDay"
        const val FIELD_BASE_SALARY = "baseSalary"
        const val FIELD_INCOME_SOURCES = "incomeSources"
        const val FIELD_GOAL = "goal"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val GOAL_NAME = "name"
        const val GOAL_TARGET_AMOUNT = "targetAmount"
        const val GOAL_SAVED_AMOUNT = "savedAmount"
        const val GOAL_TARGET_DATE = "targetDate"
        const val SAVE_FAILED_MESSAGE = "Couldn't save your plan. Please try again."
    }
}
