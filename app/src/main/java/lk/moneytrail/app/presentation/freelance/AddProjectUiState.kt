package lk.moneytrail.app.presentation.freelance

import lk.moneytrail.app.domain.model.Milestone

data class AddProjectUiState(
    val projectName: String = "",
    val clientName: String = "",
    val totalBudgetDigits: String = "",
    val milestones: List<Milestone> = emptyList(),
    val isAddMilestoneDialogOpen: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
) {
    /** The explicitly typed project budget if entered, or calculated from milestones. */
    val totalProjectPrice: Long
        get() {
            val typed = totalBudgetDigits.toLongOrNull()
            return if (typed != null && typed > 0) typed else milestones.sumOf { it.amount }
        }

    val milestoneCountText: String
        get() = "${milestones.size} added"

    val isValid: Boolean
        get() = projectName.isNotBlank() && clientName.isNotBlank() && totalProjectPrice > 0
}
