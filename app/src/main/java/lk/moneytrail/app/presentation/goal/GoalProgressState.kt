package lk.moneytrail.app.presentation.goal

import lk.moneytrail.app.domain.model.MoneyPlan
import lk.moneytrail.app.domain.model.SavingsGoal
import lk.moneytrail.app.domain.model.monthlySavingNeeded
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

data class GoalProgressState(
    val today: LocalDate = LocalDate.now(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val plan: MoneyPlan? = null,
    /** Sum of all confirmed transfers to Savings accounts. */
    val transferSavingsTotal: Long = 0L
) {
    val goal: SavingsGoal?
        get() = plan?.goal

    val goalName: String
        get() = goal?.name ?: "MacBook"

    val targetAmount: Long
        get() = goal?.targetAmount ?: 490_000L

    /** Saved so far = initial saved amount in goal + total transferred into Savings. */
    val savedAmount: Long
        get() = ((goal?.savedAmount ?: 0L) + transferSavingsTotal).coerceAtMost(targetAmount)

    val remainingAmount: Long
        get() = (targetAmount - savedAmount).coerceAtLeast(0L)

    val percentage: Double
        get() = if (targetAmount <= 0) 0.0 else ((savedAmount.toDouble() / targetAmount) * 100.0).coerceIn(0.0, 100.0)

    val progressFraction: Float
        get() = if (targetAmount <= 0) 0f else (savedAmount.toFloat() / targetAmount.toFloat()).coerceIn(0f, 1f)

    val isGoalReached: Boolean
        get() = remainingAmount <= 0L

    val targetDate: LocalDate?
        get() = goal?.targetDate

    val monthlySavingNeeded: Long
        get() {
            val g = goal ?: return 0L
            if (isGoalReached) return 0L
            // Use effective saved amount which includes transfer total
            val effectiveGoal = g.copy(savedAmount = savedAmount)
            return effectiveGoal.monthlySavingNeeded(today)
        }

    val statusMessage: String
        get() = when {
            isGoalReached -> "Goal reached! 🎉"
            targetDate != null && today.isAfter(targetDate) -> "Target date passed. LKR ${remainingAmount} remaining."
            else -> "LKR ${monthlySavingNeeded} / month needed"
        }
}
