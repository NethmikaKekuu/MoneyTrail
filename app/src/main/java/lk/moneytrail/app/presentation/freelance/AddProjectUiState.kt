package lk.moneytrail.app.presentation.freelance

import lk.moneytrail.app.domain.model.Milestone

data class AddProjectUiState(
    val projectName: String = "",
    val clientName: String = "",
    val milestones: List<Milestone> = emptyList(),
    val isAddMilestoneDialogOpen: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
) {
    val totalProjectPrice: Long
        get() = milestones.sumOf { it.amount }

    val milestoneCountText: String
        get() = "${milestones.size} added"

    val isValid: Boolean
        get() = projectName.isNotBlank() && clientName.isNotBlank() && milestones.isNotEmpty()
}
