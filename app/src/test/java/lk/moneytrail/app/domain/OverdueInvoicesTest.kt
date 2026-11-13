package lk.moneytrail.app.domain

import lk.moneytrail.app.domain.model.Milestone
import lk.moneytrail.app.domain.model.MilestoneStatus
import lk.moneytrail.app.domain.model.Project
import lk.moneytrail.app.domain.model.overdueInvoices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class OverdueInvoicesTest {

    private val today = LocalDate.of(2026, 11, 3)

    private fun millis(date: LocalDate) = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun onlyInvoicedMilestonesPastTheirDueDate_oldestFirst() {
        val projects = listOf(
            Project(
                "p1", "Bakery website", "Bakery", 120_000,
                listOf(
                    Milestone("a", "Design", 30_000, MilestoneStatus.PAID, millis(today.minusDays(30))),
                    Milestone("b", "Launch", 30_000, MilestoneStatus.INVOICED, millis(today.minusDays(5))),
                    Milestone("c", "Support", 15_000, MilestoneStatus.PLANNED, millis(today.minusDays(9)))
                )
            ),
            Project(
                "p2", "Clinic site", "Clinic", 120_000,
                listOf(
                    Milestone("d", "Booking", 50_000, MilestoneStatus.INVOICED, millis(today.minusDays(12))),
                    Milestone("e", "Portal", 70_000, MilestoneStatus.INVOICED, millis(today)),
                    Milestone("f", "Extra", 5_000, MilestoneStatus.INVOICED)
                )
            )
        )

        val overdue = projects.overdueInvoices(today)

        assertEquals(listOf("Booking", "Launch"), overdue.map { it.milestone })
        assertEquals("Clinic site", overdue.first().project)
        assertEquals(80_000L, overdue.sumOf { it.amount })
    }

    @Test
    fun noProjects_nothingOverdue() {
        assertTrue(emptyList<Project>().overdueInvoices(today).isEmpty())
    }
}
