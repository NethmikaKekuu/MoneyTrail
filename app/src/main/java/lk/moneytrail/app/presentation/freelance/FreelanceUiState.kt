package lk.moneytrail.app.presentation.freelance

import lk.moneytrail.app.presentation.components.ChipEmphasis
import lk.moneytrail.app.presentation.components.LegendItem
import lk.moneytrail.app.presentation.components.ScreenTopBar
import lk.moneytrail.app.presentation.components.StatusChip
import lk.moneytrail.app.domain.model.Milestone
import lk.moneytrail.app.domain.model.MilestoneStatus
import lk.moneytrail.app.domain.model.OverdueInvoice
import lk.moneytrail.app.domain.model.Project
import lk.moneytrail.app.domain.model.dueDate
import lk.moneytrail.app.domain.model.overdueInvoices
import lk.moneytrail.app.presentation.components.formatAmount
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** The projects list: totals across every project, overdue invoices, then one card per project. */
data class FreelanceUiState(
    val projects: List<Project> = emptyList(),
    val today: LocalDate = LocalDate.now(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    /** Invoiced and not yet paid, across all projects. */
    val totalOutstandingInvoiced: Long
        get() = projects.sumOf { it.invoicedAmount }

    val totalPaid: Long
        get() = projects.sumOf { it.paidAmount }

    /** Planned milestones not invoiced yet. */
    val totalToInvoice: Long
        get() = projects.sumOf { it.plannedAmount }

    /** Invoiced milestones due before today, by the same rule as the overdue reminder. */
    val overdueInvoices: List<OverdueInvoice>
        get() = projects.overdueInvoices(today)

    val overdueCount: Int
        get() = overdueInvoices.size

    val overdueAmount: Long
        get() = overdueInvoices.sumOf { it.amount }

    /** Projects that need attention first: overdue, then invoiced, then the rest; finished ones last. */
    val sortedProjects: List<Project>
        get() = projects.sortedWith(
            compareByDescending<Project> { it.overdueMilestones(today).isNotEmpty() }
                .thenByDescending { it.invoicedAmount > 0 }
                .thenBy { it.isComplete }
                .thenBy { it.name.lowercase(Locale.ENGLISH) }
        )
}

/** The chip on a project card. */
fun Project.statusChip(today: LocalDate): Pair<String, ChipEmphasis> {
    val overdue = overdueMilestones(today).size
    val invoiced = milestones.count { it.status == MilestoneStatus.INVOICED }
    return when {
        overdue > 0 -> "$overdue overdue" to ChipEmphasis.ALERT
        invoiced > 0 -> "$invoiced invoiced" to ChipEmphasis.ACTIVE
        isComplete -> "Completed" to ChipEmphasis.DONE
        else -> "Planned" to ChipEmphasis.QUIET
    }
}

/** "Paid 30,000 of 120,000". */
val Project.paidOfPriceText: String
    get() = "Paid ${formatAmount(paidAmount)} of ${formatAmount(totalBudget)}"

/** "3 days late", "1 day late". */
fun OverdueInvoice.lateText(today: LocalDate): String {
    val days = ChronoUnit.DAYS.between(dueDate, today)
    return if (days == 1L) "1 day late" else "$days days late"
}

/** "Due 14 Oct", "Due today", "Overdue 3 days", "Paid" or "No due date" for a milestone card. */
fun Milestone.dueLabel(today: LocalDate): String {
    if (status == MilestoneStatus.PAID) return "Paid"
    val due = dueDate ?: return "No due date"
    val late = ChronoUnit.DAYS.between(due, today)
    return when {
        status == MilestoneStatus.INVOICED && late == 1L -> "Overdue 1 day"
        status == MilestoneStatus.INVOICED && late > 1 -> "Overdue $late days"
        late == 0L -> "Due today"
        else -> "Due ${due.format(DateTimeFormatter.ofPattern(if (due.year == today.year) "d MMM" else "d MMM yyyy", Locale.ENGLISH))}"
    }
}
