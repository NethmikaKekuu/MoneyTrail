package lk.moneytrail.app.domain.repository

import kotlinx.coroutines.flow.Flow
import lk.moneytrail.app.domain.model.MoneyPlan

// Works on the signed-in user's money plan. Failures are returned as an
// lk.moneytrail.app.domain.model.AppException whose message can be shown to the user.
interface PlanRepository {

    /** Saves the pay day, salary, income sources and goal, replacing the previous plan. */
    suspend fun savePlan(plan: MoneyPlan): Result<Unit>

    /** The saved plan, or null for someone who skipped onboarding. */
    suspend fun getPlan(): Result<MoneyPlan?>

    /** Observes the saved plan in real-time. Emits null for someone who skipped onboarding. */
    fun observePlan(): Flow<Result<MoneyPlan?>>

    /** Updates the saved amount on the user's plan. */
    suspend fun updateSavedAmount(newSavedAmount: Long): Result<Unit>
}
