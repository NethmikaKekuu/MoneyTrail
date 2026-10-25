package lk.moneytrail.app.presentation.goal

import lk.moneytrail.app.domain.model.GoalModel
import java.time.LocalDate
import java.util.Locale

/**
 * Immutable UI State for Multi-Goal Progress & Allocation Screen.
 */
sealed interface GoalUiState {
    data object Loading : GoalUiState
    data object Empty : GoalUiState
    data class Error(val message: String) : GoalUiState

    data class Success(
        val goals: List<GoalModel>,
        /** This month's confirmed salary, e.g. "Salary confirmed: LKR 132,400 (bonus 12,400)"; null until then. */
        val salaryConfirmedText: String? = null,
        val totalAllocationPool: Long = 0L,
        /** The plan's pay day, offered as the transfer day for a new goal. */
        val payDay: Int? = null,
        val isTransferring: Boolean = false,
        val isNewGoalDialogOpen: Boolean = false,
        val isAllocationDialogOpen: Boolean = false,
        val bannerMessage: String? = null
    ) : GoalUiState {
        val primaryGoal: GoalModel
            get() = goals.firstOrNull() ?: GoalModel("Goal", 0L, 0L, LocalDate.now())

        val totalTargetAmount: Long
            get() = goals.sumOf { it.targetAmount }

        val totalSavedAmount: Long
            get() = goals.sumOf { it.savedAmount }

        val totalMonthlySavingNeeded: Long
            get() = goals.sumOf { it.monthlySavingNeeded }

        val overallProgressFraction: Float
            get() = if (totalTargetAmount > 0) {
                (totalSavedAmount.toFloat() / totalTargetAmount.toFloat()).coerceIn(0f, 1f)
            } else 0f

        val overallPercentageText: String
            get() = String.format(Locale.US, "%.1f%%", overallProgressFraction * 100f)
    }
}
