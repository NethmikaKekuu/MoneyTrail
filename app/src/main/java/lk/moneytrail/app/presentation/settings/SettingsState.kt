package lk.moneytrail.app.presentation.settings

import lk.moneytrail.app.domain.model.CustomCategory

data class SettingsState(
    /** True until the plan has loaded. */
    val isLoading: Boolean = true,
    /** Why the plan couldn't be loaded. */
    val loadError: String? = null,
    /** False for someone who skipped onboarding: there's no salary to change yet. */
    val hasPlan: Boolean = false,
    val payDay: Int? = null,
    val baseSalaryDigits: String = "",
    val isSavingSalary: Boolean = false,
    val salaryError: String? = null,
    /** True after the salary is saved, until it's changed again. */
    val isSalarySaved: Boolean = false,
    /** Categories the user added, A to Z. */
    val categories: List<CustomCategory> = emptyList(),
    val categoriesError: String? = null,
    /** True while the "New category" dialog is open. */
    val isAddingCategory: Boolean = false,
    val isSavingCategory: Boolean = false,
    val categoryError: String? = null,
    /** The category waiting for the user to confirm it should be deleted. */
    val categoryToDelete: CustomCategory? = null
) {
    val baseSalary: Long?
        get() = baseSalaryDigits.toLongOrNull()
}
