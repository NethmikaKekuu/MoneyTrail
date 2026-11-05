package lk.moneytrail.app.domain.model

import java.time.ZoneId
import java.time.LocalDate
import java.time.Instant
import java.util.Locale

/** Status of a freelance project milestone: planned, invoiced, or paid. */
enum class MilestoneStatus(val value: String) {
    PLANNED("planned"),
    INVOICED("invoiced"),
    PAID("paid");

    companion object {
        fun fromString(value: String?): MilestoneStatus = when (value?.lowercase(Locale.ENGLISH)?.trim()) {
            "invoiced" -> INVOICED
            "paid" -> PAID
            else -> PLANNED
        }
    }
}

/** One milestone inside a freelance project. */
data class Milestone(
    val id: String = "",
    val title: String,
    /** Amount in whole rupees (LKR). */
    val amount: Long,
    val status: MilestoneStatus = MilestoneStatus.PLANNED,
    /** Due date in epoch milliseconds. */
    val dueTimestamp: Long = 0L
)

/** A freelance project with client details, budget, and milestones. */
data class Project(
    val id: String = "",
    val name: String,
    val client: String,
    /** Total agreed project budget in whole rupees (LKR). */
    val totalBudget: Long,
    val milestones: List<Milestone> = emptyList()
) {
    val totalMilestonesAmount: Long
        get() = milestones.sumOf { it.amount }

    val paidAmount: Long
        get() = milestones.filter { it.status == MilestoneStatus.PAID }.sumOf { it.amount }

    val invoicedAmount: Long
        get() = milestones.filter { it.status == MilestoneStatus.INVOICED }.sumOf { it.amount }

    val plannedAmount: Long
        get() = milestones.filter { it.status == MilestoneStatus.PLANNED }.sumOf { it.amount }

    /** Part of the price not yet split into milestones; negative if the milestones add up to more. */
    val unsplitAmount: Long
        get() = totalBudget - totalMilestonesAmount

    /** Every milestone paid (and there is at least one). */
    val isComplete: Boolean
        get() = milestones.isNotEmpty() && milestones.all { it.status == MilestoneStatus.PAID }

    /** Invoiced milestones whose due day has passed, oldest first. */
    fun overdueMilestones(today: LocalDate): List<Milestone> =
        milestones.filter { it.isOverdue(today) }.sortedBy { it.dueTimestamp }

    /** Milestones in the order they fall due; ones without a date go last. */
    val milestonesByDue: List<Milestone>
        get() = milestones.sortedBy { if (it.dueTimestamp > 0) it.dueTimestamp else Long.MAX_VALUE }
}

/** The milestone's due day, or null if it has none. */
val Milestone.dueDate: LocalDate?
    get() = if (dueTimestamp > 0) Instant.ofEpochMilli(dueTimestamp).atZone(ZoneId.systemDefault()).toLocalDate() else null

/** Invoiced and due before [today]. Due today is not late yet. */
fun Milestone.isOverdue(today: LocalDate): Boolean =
    status == MilestoneStatus.INVOICED && dueDate?.isBefore(today) == true

/** Start of [date] in this phone's time zone, as stored in [Milestone.dueTimestamp]. */
fun dueTimestampOf(date: LocalDate): Long =
    date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

/** An invoiced milestone whose due date has passed without being paid. */
data class OverdueInvoice(
    val project: String,
    val milestone: String,
    val amount: Long,
    val dueDate: LocalDate,
    val projectId: String = ""
)

/**
 * Every invoiced milestone due before [today], oldest first: the follow-ups Kavindu
 * forgot twice. A milestone without a due date can't be late.
 */
fun List<Project>.overdueInvoices(today: LocalDate): List<OverdueInvoice> =
    flatMap { project ->
        project.milestones
            .filter { it.isOverdue(today) }
            .map { m -> OverdueInvoice(project.name, m.title, m.amount, m.dueDate!!, project.id) }
    }
        .sortedBy { it.dueDate }
