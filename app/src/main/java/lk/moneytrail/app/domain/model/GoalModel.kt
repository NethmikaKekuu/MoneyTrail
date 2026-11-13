package lk.moneytrail.app.domain.model

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Domain model representing goal progress (e.g. MacBook Pro goal targeting LKR 490,000).
 */
data class GoalModel(
    val name: String,
    /** In whole rupees (LKR). E.g. 490,000 */
    val targetAmount: Long,
    /** In whole rupees (LKR). E.g. 11,200 */
    val savedAmount: Long,
    val targetDate: LocalDate,
    val today: LocalDate = LocalDate.now(),
    /** The [SavingsGoal.id] it shows. */
    val id: String = ""
) {
    /** Remaining amount in LKR to reach the target amount. */
    val remainingAmount: Long
        get() = (targetAmount - savedAmount).coerceAtLeast(0L)

    val isReached: Boolean
        get() = savedAmount >= targetAmount

    /** The target date has passed and the goal isn't reached yet. */
    val isOverdue: Boolean
        get() = !isReached && YearMonth.from(targetDate).isBefore(YearMonth.from(today))

    /** Progress fraction from 0.0 to 1.0. */
    val progressFraction: Float
        get() = if (targetAmount > 0) {
            (savedAmount.toFloat() / targetAmount.toFloat()).coerceIn(0f, 1f)
        } else 0f

    /** Progress formatted as percentage string (e.g. "2.3%"). */
    val percentageText: String
        get() = String.format(Locale.US, "%.1f%%", progressFraction * 100f)

    /** Months left until target date, counting current month. Minimum 1 month. */
    val monthsRemaining: Long
        get() = ChronoUnit.MONTHS.between(YearMonth.from(today), YearMonth.from(targetDate)).coerceAtLeast(1L)

    /**
     * Dynamically calculated monthly saving needed to reach the target by deadline.
     * Formula: Remaining amount / months left.
     */
    val monthlySavingNeeded: Long
        get() {
            if (remainingAmount <= 0) return 0L
            return (remainingAmount + monthsRemaining - 1) / monthsRemaining
        }

    /** Formatted timeline summary text (e.g. "12 months remaining until 19 Oct 2027") */
    val timelineSummary: String
        get() {
            val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
            return "$monthsRemaining months remaining until ${targetDate.format(formatter)}"
        }

    /** Formula breakdown string for display */
    val formulaExplanation: String
        get() = "Shortfall LKR %,d ÷ $monthsRemaining months = LKR %,d/mo".format(Locale.US, remainingAmount, monthlySavingNeeded)

    companion object {
        fun fromSavingsGoal(goal: SavingsGoal, today: LocalDate = LocalDate.now()): GoalModel =
            GoalModel(
                name = goal.name,
                targetAmount = goal.targetAmount,
                savedAmount = goal.savedAmount,
                targetDate = goal.targetDate,
                today = today,
                id = goal.id
            )
    }
}
