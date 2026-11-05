package lk.moneytrail.app.presentation.freelance

import lk.moneytrail.app.domain.model.Milestone
import lk.moneytrail.app.domain.model.MilestoneStatus
import lk.moneytrail.app.domain.model.Project
import lk.moneytrail.app.domain.model.dueDate
import java.time.LocalDate

data class ProjectDetailUiState(
    val project: Project? = null,
    val today: LocalDate = LocalDate.now(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    /** Shown in the error view when the project can't be loaded or was removed. */
    val loadError: String? = null,
    /** One-off message for a snackbar. */
    val message: String? = null,
    /** Set after a change that can be undone; the snackbar offers Undo. */
    val undoMessage: String? = null,
    val editingMilestone: Milestone? = null,
    val isAddMilestoneOpen: Boolean = false,
    val isDeleteConfirmOpen: Boolean = false
) {
    /** Milestones in due order. */
    val milestones: List<Milestone>
        get() = project?.milestonesByDue.orEmpty()

    /** Price not yet split into milestones. */
    val leftToSplit: Long
        get() = project?.unsplitAmount ?: 0L

    /** Adding always works; past the price, the price goes up to match. */
    val canAddMilestone: Boolean
        get() = !isSaving && project != null

    /** Cap for a new milestone: what's left of the price, or none once it's fully split. */
    val addMaxAmount: Long?
        get() = leftToSplit.takeIf { it > 0 }

    /** The most an edited milestone may be: its own amount plus what's left of the price. */
    fun maxAmountFor(milestone: Milestone): Long = milestone.amount + leftToSplit.coerceAtLeast(0L)

    /** Record payment works for anything not paid yet. */
    val hasUnpaid: Boolean
        get() = project?.milestones?.any { it.status != MilestoneStatus.PAID } == true

    fun nextDueDate(): LocalDate =
        (milestones.mapNotNull { it.dueDate }.maxOrNull()?.takeIf { it.isAfter(today) } ?: today).plusWeeks(2)
}
