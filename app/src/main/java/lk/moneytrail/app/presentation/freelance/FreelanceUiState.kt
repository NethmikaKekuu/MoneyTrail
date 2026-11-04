package lk.moneytrail.app.presentation.freelance

import lk.moneytrail.app.domain.model.Milestone
import lk.moneytrail.app.domain.model.MilestoneStatus
import lk.moneytrail.app.domain.model.Project

data class FreelanceUiState(
    val projects: List<Project> = emptyList(),
    val selectedProjectId: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val undoMessage: String? = null,
    val editingMilestone: Milestone? = null,
    val draftTitle: String = "",
    val draftAmountDigits: String = ""
) {
    val selectedProject: Project?
        get() = projects.find { it.id == selectedProjectId } ?: projects.firstOrNull()

    /** Sum of all invoiced, unpaid milestone amounts across all projects. */
    val totalOutstandingInvoiced: Long
        get() = projects.sumOf { p ->
            p.milestones.filter { it.status == MilestoneStatus.INVOICED }.sumOf { it.amount }
        }

    /** Count of invoiced milestones past due date across all projects. */
    val overdueCount: Int
        get() {
            val now = System.currentTimeMillis()
            return projects.sumOf { p ->
                p.milestones.count { m ->
                    m.status == MilestoneStatus.INVOICED && m.dueTimestamp > 0 && m.dueTimestamp < now
                }
            }
        }

    /** Enabled only when a PLANNED milestone exists on the selected project and not saving. */
    val isMarkInvoicedEnabled: Boolean
        get() = !isSaving && selectedProject?.milestones?.any { it.status == MilestoneStatus.PLANNED } == true

    /** Enabled only when an INVOICED milestone exists on the selected project and not saving. */
    val isRecordPaymentEnabled: Boolean
        get() = !isSaving && selectedProject?.milestones?.any { it.status == MilestoneStatus.INVOICED } == true

    val isEditingMilestone: Boolean
        get() = editingMilestone != null
}
