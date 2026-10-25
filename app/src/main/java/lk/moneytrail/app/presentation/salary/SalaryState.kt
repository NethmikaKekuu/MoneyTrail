package lk.moneytrail.app.presentation.salary

data class SalaryState(
    /** True until the plan has loaded. */
    val isLoading: Boolean = true,
    /** Why the plan couldn't be loaded. */
    val loadError: String? = null,
    /** False for someone who skipped onboarding: there's no salary to change yet. */
    val hasPlan: Boolean = true,
    /** The pay day and base salary as saved. */
    val payDay: Int? = null,
    val baseSalary: Long = 0L,
    /** The pay day picked on the calendar grid, and the base salary typed, before saving. */
    val selectedPayDay: Int? = null,
    val baseSalaryDigits: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    /** True right after saving, until something changes again. */
    val isSaved: Boolean = false
) {
    /** True when the pay day or base salary differ from what's saved, to show Save. */
    val isChanged: Boolean
        get() = selectedPayDay != payDay || (baseSalaryDigits.toLongOrNull() ?: 0L) != baseSalary
}
