package lk.moneytrail.app.presentation.goal

import lk.moneytrail.app.domain.model.GoalModel
import lk.moneytrail.app.domain.model.SavingsGoal
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
        /** The account this month's salary went into; null when it isn't confirmed yet. */
        val fromAccount: String? = null,
        /** This month's saving for each goal not yet reached, LKR by goal id. */
        val savingsToMove: Map<String, Long> = emptyMap(),
        /** The total of [savingsToMove]. */
        val totalAllocationPool: Long = 0L,
        /** True once "I've moved it" was done this month. */
        val isSavingsMovedThisMonth: Boolean = false,
        /** When next month's saving is due, for "Done for this month". */
        val nextSavingsDate: LocalDate? = null,
        /** The plan's pay day, offered as the transfer day for a new goal. */
        val payDay: Int? = null,
        val isTransferring: Boolean = false,
        val isNewGoalDialogOpen: Boolean = false,
        /** The goal being edited in the goal dialog; null when adding a new one. */
        val editingGoal: SavingsGoal? = null,
        val isAllocationDialogOpen: Boolean = false,
        /** The goal waiting for "Remove this goal?" to be confirmed. */
        val deletingGoalId: String? = null,
        /** True once "Remind me tomorrow" is saved, so the screen can leave. */
        val isReminderSet: Boolean = false,
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

        /** True when there's still something to move this month: the "Move LKR … to Savings now?" card. */
        val isSavingsToDo: Boolean
            get() = !isSavingsMovedThisMonth && totalAllocationPool > 0

        val areAllGoalsReached: Boolean
            get() = goals.isNotEmpty() && goals.all { it.isReached }
    }
}
