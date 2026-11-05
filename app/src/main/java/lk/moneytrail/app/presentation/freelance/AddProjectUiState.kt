package lk.moneytrail.app.presentation.freelance

import lk.moneytrail.app.domain.model.Milestone
import lk.moneytrail.app.domain.model.dueDate
import lk.moneytrail.app.presentation.components.formatAmount
import java.time.LocalDate

data class AddProjectUiState(
    val projectName: String = "",
    val clientName: String = "",
    /** Exactly what the user typed in the price box; never filled in for them. */
    val totalBudgetDigits: String = "",
    val milestones: List<Milestone> = emptyList(),
    val isAddMilestoneDialogOpen: Boolean = false,
    /** Asked before saving when the milestones add up to less than the price. */
    val isShortSplitDialogOpen: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
) {
    /** The price the user typed, 0 until they type one. */
    val totalProjectPrice: Long
        get() = totalBudgetDigits.toLongOrNull() ?: 0L

    val splitAmount: Long
        get() = milestones.sumOf { it.amount }

    /** Price not yet split into milestones; negative when the milestones add up to more. */
    val leftToSplit: Long
        get() = totalProjectPrice - splitAmount

    /** "Split 40,000 of 60,000 · 20,000 left", or null before a price is typed. */
    val splitText: String?
        get() = when {
            totalProjectPrice <= 0 -> null
            leftToSplit == 0L -> "Fully split: ${formatAmount(splitAmount)} of ${formatAmount(totalProjectPrice)}"
            leftToSplit < 0 -> "Milestones are ${formatAmount(-leftToSplit)} over the price"
            else -> "Split ${formatAmount(splitAmount)} of ${formatAmount(totalProjectPrice)} · ${formatAmount(leftToSplit)} left"
        }

    val isOverSplit: Boolean
        get() = totalProjectPrice > 0 && leftToSplit < 0

    val milestoneCountText: String
        get() = "${milestones.size} added"

    val isValid: Boolean
        get() = saveError == null

    /** Why the project can't be saved yet, or null. */
    val saveError: String?
        get() = when {
            projectName.isBlank() -> "Enter a project name."
            clientName.isBlank() -> "Enter the client name."
            totalProjectPrice <= 0 -> "Enter the total project price."
            isOverSplit -> "Milestones add up to ${formatAmount(splitAmount)}, more than the price. Remove one or raise the price."
            else -> null
        }

    /** Message for the "milestones fall short" question. */
    val shortSplitText: String
        get() = "Milestones add up to ${formatAmount(splitAmount)}, ${formatAmount(leftToSplit)} short of the price."

    /** A new milestone falls due two weeks after the latest one (or after today). */
    fun nextDueDate(today: LocalDate): LocalDate =
        (milestones.mapNotNull { it.dueDate }.maxOrNull()?.takeIf { it.isAfter(today) } ?: today).plusWeeks(2)
}
