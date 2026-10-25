package lk.moneytrail.app.presentation.salary

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
    /** The usual pay before bonuses, as saved. 0 when no salary is set up. */
    val baseSalary: Long = 0L,
    /** What arrived minus the base: negative when less than the base arrived. */
    val bonusAmount: Long = 0L,
    val amountCreditedDigits: String = "",
    /** False when there's no base salary and none on the calendar to confirm. */
    val hasSalary: Boolean = true,
    /** False for someone who skipped onboarding: there's no salary or goal to change yet. */
    val hasPlan: Boolean = true,
    /** The pay day as saved. */
    val payDay: Int? = null,
    /** The pay day picked on the calendar grid, and the base salary typed, before saving. */
    val selectedPayDay: Int? = null,
    val baseSalaryDigits: String = "",
    val isSavingSalary: Boolean = false,
    val salarySettingsError: String? = null,
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
) {
    val totalCredited: Long
        get() = amountCreditedDigits.toLongOrNull() ?: 0L

    /** True when the pay day or base salary differ from what's saved, to show Save. */
    val isSalaryChanged: Boolean
        get() = selectedPayDay != payDay || (baseSalaryDigits.toLongOrNull() ?: 0L) != baseSalary
}
