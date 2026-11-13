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

    /**
     * The saved plan, or null for someone who skipped onboarding. Updates live whenever
     * the user's plan or savings goal is changed in Firestore.
     */
    fun observePlan(): Flow<Result<MoneyPlan?>>
}
