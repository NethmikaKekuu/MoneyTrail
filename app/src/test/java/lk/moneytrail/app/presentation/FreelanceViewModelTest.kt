package lk.moneytrail.app.presentation

import lk.moneytrail.app.domain.model.Milestone
import lk.moneytrail.app.domain.model.MilestoneStatus
import lk.moneytrail.app.domain.model.Project
import lk.moneytrail.app.domain.model.dueTimestampOf
import lk.moneytrail.app.presentation.freelance.ChipEmphasis
import lk.moneytrail.app.presentation.freelance.FreelanceUiState
import lk.moneytrail.app.presentation.freelance.ProjectDetailUiState
import lk.moneytrail.app.presentation.freelance.dueLabel
import lk.moneytrail.app.presentation.freelance.lateText
import lk.moneytrail.app.presentation.freelance.statusChip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FreelanceViewModelTest {

    private val today = LocalDate.of(2026, 11, 5)
    private fun m(id: String, amount: Long, status: MilestoneStatus, due: LocalDate?) =
        Milestone(id, "M $id", amount, status, due?.let(::dueTimestampOf) ?: 0L)

    private val bakery = Project(
        "p1", "Bakery website", "Sweet Crumbs", 120_000L,
        listOf(
            m("1", 30_000L, MilestoneStatus.PAID, today.minusDays(40)),
            m("2", 45_000L, MilestoneStatus.INVOICED, today.minusDays(3)),
            m("3", 15_000L, MilestoneStatus.INVOICED, today),
            m("4", 20_000L, MilestoneStatus.PLANNED, today.plusDays(20))
        )
    )
    private val clinic = Project("p2", "Clinic", "Dr. Perera", 50_000L, listOf(m("5", 50_000L, MilestoneStatus.PAID, today.minusDays(5))))

    @Test
    fun totals_acrossAllProjects() {
        val state = FreelanceUiState(projects = listOf(bakery, clinic), today = today)
        assertEquals(60_000L, state.totalOutstandingInvoiced)
        assertEquals(80_000L, state.totalPaid)
        assertEquals(20_000L, state.totalToInvoice)
    }

    @Test
    fun overdue_countsOnlyInvoicedPastTheirDueDay_notDueToday() {
        val state = FreelanceUiState(projects = listOf(bakery, clinic), today = today)
        assertEquals(1, state.overdueCount)
        assertEquals(45_000L, state.overdueAmount)
        assertEquals("p1", state.overdueInvoices.single().projectId)
        assertEquals("3 days late", state.overdueInvoices.single().lateText(today))
    }

    @Test
    fun projectsNeedingAttentionComeFirst() {
        val state = FreelanceUiState(projects = listOf(clinic, bakery), today = today)
        assertEquals(listOf("p1", "p2"), state.sortedProjects.map { it.id })
    }

    @Test
    fun statusChip_overdueThenInvoicedThenCompleted() {
        assertEquals("1 overdue" to ChipEmphasis.ALERT, bakery.statusChip(today))
        assertEquals("Completed" to ChipEmphasis.DONE, clinic.statusChip(today))
        val invoicedOnly = bakery.copy(milestones = listOf(m("x", 1L, MilestoneStatus.INVOICED, today.plusDays(1))))
        assertEquals("1 invoiced" to ChipEmphasis.ACTIVE, invoicedOnly.statusChip(today))
        assertEquals("Planned" to ChipEmphasis.QUIET, bakery.copy(milestones = listOf(m("y", 1L, MilestoneStatus.PLANNED, null))).statusChip(today))
    }

    @Test
    fun dueLabel_usesTheRealDate() {
        assertEquals("Overdue 3 days", bakery.milestones[1].dueLabel(today))
        assertEquals("Due today", bakery.milestones[2].dueLabel(today))
        assertEquals("Due 25 Nov", bakery.milestones[3].dueLabel(today))
        assertEquals("Paid", bakery.milestones[0].dueLabel(today))
        assertEquals("No due date", m("z", 1L, MilestoneStatus.PLANNED, null).dueLabel(today))
    }

    @Test
    fun detail_addAndEditAreLimitedByThePrice() {
        val state = ProjectDetailUiState(project = bakery.copy(totalBudget = 130_000L), today = today, isLoading = false)
        assertEquals(20_000L, state.leftToSplit)
        assertTrue(state.canAddMilestone)
        assertEquals(35_000L, state.maxAmountFor(bakery.milestones[2]))
        assertTrue(state.hasUnpaid)

        val full = ProjectDetailUiState(project = bakery.copy(totalBudget = 110_000L), today = today, isLoading = false)
        assertFalse(full.canAddMilestone)
        assertFalse(ProjectDetailUiState(project = clinic, isLoading = false).hasUnpaid)
    }

    @Test
    fun detail_listsMilestonesInDueOrder() {
        val shuffled = bakery.copy(milestones = bakery.milestones.reversed() + m("n", 1L, MilestoneStatus.PLANNED, null))
        val ids = ProjectDetailUiState(project = shuffled, isLoading = false).milestones.map { it.id }
        assertEquals(listOf("1", "2", "3", "4", "n"), ids)
    }
}
