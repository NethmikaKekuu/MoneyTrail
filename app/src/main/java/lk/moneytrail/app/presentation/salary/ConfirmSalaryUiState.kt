package lk.moneytrail.app.presentation.salary

data class BonusHistoryItem(
    val monthLabel: String,
    val amount: Long,
    val isCurrentMonth: Boolean = false
)

// Every value comes from the user's plan and entries; nothing about a real person is
// filled in here.
data class ConfirmSalaryUiState(
    /** "October salary". */
    val monthTitle: String = "",
    /** "Due 25 Oct", "Due today", "Overdue", "Not confirmed yet" or "Confirmed". */
    val statusText: String = "",
    /** True once this month's salary is confirmed; the button then corrects it. */
    val isConfirmed: Boolean = false,
    val accountName: String = "",
    /** The usual pay before bonuses. 0 when no salary is set up. */
    val baseSalary: Long = 0L,
    /** What arrived minus the base: negative when less than the base arrived. */
    val bonusAmount: Long = 0L,
    val amountCreditedDigits: String = "",
    /** False when there's no base salary and none on the calendar to confirm. */
    val hasSalary: Boolean = true,
    /** False for someone who skipped onboarding: there's no salary or goal to change yet. */
    val hasPlan: Boolean = true,
    /** The plan's pay day, for "25th of the month". */
    val payDay: Int? = null,
    /** True while the "Pay day and base salary" dialog is open. */
    val isEditingSalary: Boolean = false,
    val payDayDigits: String = "",
    val baseSalaryDigits: String = "",
    val isSavingSalary: Boolean = false,
    val salarySettingsError: String? = null,
    val bonusHistory: List<BonusHistoryItem> = emptyList(),
    val averageBonus: Long = 0L,
    val highestBonus: Long = 0L,
    val highestBonusMonth: String = "",
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
) {
    val totalCredited: Long
        get() = amountCreditedDigits.toLongOrNull() ?: 0L

    val hasBonusHistory: Boolean
        get() = bonusHistory.any { it.amount > 0 }
}
