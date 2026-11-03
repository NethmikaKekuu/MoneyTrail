package lk.moneytrail.app.domain.model

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
}
