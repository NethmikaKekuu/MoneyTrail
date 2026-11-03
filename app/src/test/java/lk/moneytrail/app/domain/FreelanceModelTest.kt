package lk.moneytrail.app.domain

import lk.moneytrail.app.domain.model.Milestone
import lk.moneytrail.app.domain.model.MilestoneStatus
import lk.moneytrail.app.domain.model.Project
import org.junit.Assert.assertEquals
import org.junit.Test

class FreelanceModelTest {

    @Test
    fun milestoneStatus_fromString_parsesCorrectly() {
        assertEquals(MilestoneStatus.PLANNED, MilestoneStatus.fromString("planned"))
        assertEquals(MilestoneStatus.PLANNED, MilestoneStatus.fromString("PLANNED"))
        assertEquals(MilestoneStatus.INVOICED, MilestoneStatus.fromString("invoiced"))
        assertEquals(MilestoneStatus.INVOICED, MilestoneStatus.fromString("INVOICED"))
        assertEquals(MilestoneStatus.PAID, MilestoneStatus.fromString("paid"))
        assertEquals(MilestoneStatus.PAID, MilestoneStatus.fromString("PAID"))
        assertEquals(MilestoneStatus.PLANNED, MilestoneStatus.fromString(null))
        assertEquals(MilestoneStatus.PLANNED, MilestoneStatus.fromString("unknown"))
    }

    @Test
    fun project_calculatesMilestoneTotalsAndStatusBreakdowns() {
        val milestone1 = Milestone(
            id = "m1",
            title = "UI Wireframes",
            amount = 50_000L,
            status = MilestoneStatus.PAID,
            dueTimestamp = 1700000000000L
        )
        val milestone2 = Milestone(
            id = "m2",
            title = "Frontend Development",
            amount = 100_000L,
            status = MilestoneStatus.INVOICED,
            dueTimestamp = 1701000000000L
        )
        val milestone3 = Milestone(
            id = "m3",
            title = "Backend Integration",
            amount = 80_000L,
            status = MilestoneStatus.PLANNED,
            dueTimestamp = 1702000000000L
        )

        val project = Project(
            id = "p1",
            name = "E-Commerce App",
            client = "Acme Corp",
            totalBudget = 250_000L,
            milestones = listOf(milestone1, milestone2, milestone3)
        )

        assertEquals("p1", project.id)
        assertEquals("E-Commerce App", project.name)
        assertEquals("Acme Corp", project.client)
        assertEquals(250_000L, project.totalBudget)

        assertEquals(230_000L, project.totalMilestonesAmount)
        assertEquals(50_000L, project.paidAmount)
        assertEquals(100_000L, project.invoicedAmount)
        assertEquals(80_000L, project.plannedAmount)
    }

    @Test
    fun markingMilestoneAsPaid_updatesPaidAndInvoicedAmountsCorrectly() {
        val milestone1 = Milestone("m1", "Design", 30_000L, MilestoneStatus.PAID)
        val milestone2 = Milestone("m2", "Build", 45_000L, MilestoneStatus.PAID)
        val milestone3 = Milestone("m3", "Launch", 30_000L, MilestoneStatus.INVOICED)
        val milestone4 = Milestone("m4", "Support", 15_000L, MilestoneStatus.PLANNED)

        val project = Project("p1", "Bakery website", "React build", 120_000L, listOf(milestone1, milestone2, milestone3, milestone4))

        // Initial state before payment: Paid = 75,000, Invoiced = 30,000
        assertEquals(75_000L, project.paidAmount)
        assertEquals(30_000L, project.invoicedAmount)
        assertEquals(15_000L, project.plannedAmount)

        // Settle milestone 3 ("Launch") to PAID
        val updatedMilestones = project.milestones.map {
            if (it.id == "m3") it.copy(status = MilestoneStatus.PAID) else it
        }
        val updatedProject = project.copy(milestones = updatedMilestones)

        // Updated state after payment: Paid = 105,000, Invoiced = 0
        assertEquals(105_000L, updatedProject.paidAmount)
        assertEquals(0L, updatedProject.invoicedAmount)
        assertEquals(15_000L, updatedProject.plannedAmount)
    }

    @Test
    fun project_withEmptyMilestones_returnsZeroTotals() {
        val project = Project(
            id = "p2",
            name = "Logo Design",
            client = "Startup LLC",
            totalBudget = 30_000L,
            milestones = emptyList()
        )

        assertEquals(0L, project.totalMilestonesAmount)
        assertEquals(0L, project.paidAmount)
        assertEquals(0L, project.invoicedAmount)
        assertEquals(0L, project.plannedAmount)
    }
}
