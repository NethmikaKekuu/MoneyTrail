package lk.moneytrail.app.presentation

import lk.moneytrail.app.domain.model.Milestone
import lk.moneytrail.app.domain.model.MilestoneStatus
import lk.moneytrail.app.domain.model.Project
import lk.moneytrail.app.presentation.freelance.FreelanceUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FreelanceViewModelTest {

    @Test
    fun freelanceUiState_calculatesOutstandingInvoicedAmount() {
        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L

        val milestones = listOf(
            Milestone("m1", "Design", 30_000L, MilestoneStatus.PAID, now - (30 * dayMs)),
            Milestone("m2", "Build", 45_000L, MilestoneStatus.PAID, now - (15 * dayMs)),
            Milestone("m3", "Launch", 30_000L, MilestoneStatus.INVOICED, now - (5 * dayMs)),
            Milestone("m4", "Support", 15_000L, MilestoneStatus.PLANNED, now + (20 * dayMs))
        )

        val project = Project("p1", "Bakery website", "React build", 120_000L, milestones)
        val state = FreelanceUiState(projects = listOf(project), selectedProjectId = "p1")

        assertEquals("p1", state.selectedProject?.id)
        assertEquals("Bakery website", state.selectedProject?.name)
        assertEquals(30_000L, state.totalOutstandingInvoiced)
        assertEquals(1, state.overdueCount)
    }

    @Test
    fun freelanceUiState_dynamicButtonEnabledStates() {
        val plannedMilestone = Milestone("m1", "Support", 15_000L, MilestoneStatus.PLANNED)
        val invoicedMilestone = Milestone("m2", "Launch", 30_000L, MilestoneStatus.INVOICED)
        val paidMilestone = Milestone("m3", "Design", 30_000L, MilestoneStatus.PAID)

        // Case 1: Has PLANNED and INVOICED milestones
        val projectBoth = Project("p1", "Project A", "Client", 75_000L, listOf(plannedMilestone, invoicedMilestone))
        val stateBoth = FreelanceUiState(projects = listOf(projectBoth), selectedProjectId = "p1")
        assertTrue(stateBoth.isMarkInvoicedEnabled)
        assertTrue(stateBoth.isRecordPaymentEnabled)

        // Case 2: Only PAID milestones
        val projectPaidOnly = Project("p2", "Project B", "Client", 30_000L, listOf(paidMilestone))
        val statePaidOnly = FreelanceUiState(projects = listOf(projectPaidOnly), selectedProjectId = "p2")
        assertFalse(statePaidOnly.isMarkInvoicedEnabled)
        assertFalse(statePaidOnly.isRecordPaymentEnabled)

        // Case 3: Only PLANNED milestones
        val projectPlannedOnly = Project("p3", "Project C", "Client", 15_000L, listOf(plannedMilestone))
        val statePlannedOnly = FreelanceUiState(projects = listOf(projectPlannedOnly), selectedProjectId = "p3")
        assertTrue(statePlannedOnly.isMarkInvoicedEnabled)
        assertFalse(statePlannedOnly.isRecordPaymentEnabled)
    }

    @Test
    fun editingMilestone_preservesExistingStatus() {
        val originalMilestone = Milestone("m2", "Launch", 30_000L, MilestoneStatus.INVOICED)

        val editedMilestone = originalMilestone.copy(
            title = "Launch Phase 2",
            amount = 35_000L
        )

        assertEquals("m2", editedMilestone.id)
        assertEquals("Launch Phase 2", editedMilestone.title)
        assertEquals(35_000L, editedMilestone.amount)
        assertEquals(MilestoneStatus.INVOICED, editedMilestone.status)
    }

    @Test
    fun markingMilestoneAsPaid_updatesOutstandingBalanceAndOverdueCount() {
        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L

        val m1 = Milestone("m1", "Design", 30_000L, MilestoneStatus.PAID, now - (30 * dayMs))
        val m2 = Milestone("m2", "Build", 45_000L, MilestoneStatus.PAID, now - (15 * dayMs))
        val m3 = Milestone("m3", "Launch", 30_000L, MilestoneStatus.INVOICED, now - (5 * dayMs))
        val m4 = Milestone("m4", "Support", 15_000L, MilestoneStatus.PLANNED, now + (20 * dayMs))

        val project = Project("p1", "Bakery website", "React build", 120_000L, listOf(m1, m2, m3, m4))
        val initialState = FreelanceUiState(projects = listOf(project), selectedProjectId = "p1")

        assertEquals(30_000L, initialState.totalOutstandingInvoiced)
        assertEquals(1, initialState.overdueCount)

        val updatedMilestones = listOf(m1, m2, m3.copy(status = MilestoneStatus.PAID), m4)
        val updatedProject = project.copy(milestones = updatedMilestones)
        val updatedState = FreelanceUiState(projects = listOf(updatedProject), selectedProjectId = "p1")

        assertEquals(0L, updatedState.totalOutstandingInvoiced)
        assertEquals(0, updatedState.overdueCount)
    }

    @Test
    fun deletingProject_removesProjectFromUiState() {
        val p1 = Project("p1", "Bakery website", "React build", 120_000L)
        val p2 = Project("p2", "Clinic booking site", "Next.js", 100_000L)

        val initialState = FreelanceUiState(
            projects = listOf(p1, p2),
            selectedProjectId = "p1"
        )

        val updatedProjects = initialState.projects.filterNot { it.id == "p1" }
        val updatedState = initialState.copy(
            projects = updatedProjects,
            selectedProjectId = updatedProjects.firstOrNull()?.id,
            undoMessage = "Project deleted."
        )

        assertEquals(1, updatedState.projects.size)
        assertEquals("p2", updatedState.selectedProject?.id)
        assertEquals("Project deleted.", updatedState.undoMessage)
    }

    @Test
    fun milestoneStateTransitions_forwardAndUndoFlows() {
        val m1 = Milestone("m1", "Design", 30_000L, MilestoneStatus.PLANNED)
        val m2 = Milestone("m2", "Build", 45_000L, MilestoneStatus.INVOICED)
        val m3 = Milestone("m3", "Launch", 30_000L, MilestoneStatus.PAID)

        val project = Project("p1", "Bakery website", "React build", 105_000L, listOf(m1, m2, m3))

        val invoicedMilestones = project.milestones.map {
            if (it.status == MilestoneStatus.PLANNED) it.copy(status = MilestoneStatus.INVOICED) else it
        }
        assertEquals(MilestoneStatus.INVOICED, invoicedMilestones[0].status)

        val paidMilestones = project.milestones.map {
            if (it.status == MilestoneStatus.INVOICED) it.copy(status = MilestoneStatus.PAID) else it
        }
        assertEquals(MilestoneStatus.PAID, paidMilestones[1].status)

        val undonePaid = m3.copy(
            status = when (m3.status) {
                MilestoneStatus.PAID -> MilestoneStatus.INVOICED
                MilestoneStatus.INVOICED -> MilestoneStatus.PLANNED
                MilestoneStatus.PLANNED -> MilestoneStatus.PLANNED
            }
        )
        assertEquals(MilestoneStatus.INVOICED, undonePaid.status)

        val undoneInvoiced = m2.copy(
            status = when (m2.status) {
                MilestoneStatus.PAID -> MilestoneStatus.INVOICED
                MilestoneStatus.INVOICED -> MilestoneStatus.PLANNED
                MilestoneStatus.PLANNED -> MilestoneStatus.PLANNED
            }
        )
        assertEquals(MilestoneStatus.PLANNED, undoneInvoiced.status)
    }

    @Test
    fun freelanceUiState_undoMessageState() {
        val state = FreelanceUiState(
            undoMessage = "Marked 'Launch' as Invoiced."
        )

        assertEquals("Marked 'Launch' as Invoiced.", state.undoMessage)
    }

    @Test
    fun freelanceUiState_handlesLoadingAndErrorStates() {
        val loadingState = FreelanceUiState(isLoading = true)
        val errorState = FreelanceUiState(errorMessage = "Network error")

        assertTrue(loadingState.isLoading)
        assertFalse(errorState.isLoading)
        assertEquals("Network error", errorState.errorMessage)
    }
}
