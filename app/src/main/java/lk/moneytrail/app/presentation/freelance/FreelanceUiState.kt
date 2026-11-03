package lk.moneytrail.app.presentation.freelance

import lk.moneytrail.app.domain.model.MilestoneStatus
import lk.moneytrail.app.domain.model.Project

data class FreelanceUiState(
    val projects: List<Project> = emptyList(),
    val selectedProjectId: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
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
}
