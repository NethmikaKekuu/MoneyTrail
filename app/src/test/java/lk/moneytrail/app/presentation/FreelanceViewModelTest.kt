package lk.moneytrail.app.presentation

import lk.moneytrail.app.domain.model.Milestone
import lk.moneytrail.app.domain.model.MilestoneStatus
import lk.moneytrail.app.domain.model.Project
import lk.moneytrail.app.presentation.freelance.FreelanceUiState
import lk.moneytrail.app.presentation.freelance.FreelanceViewModel
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
    fun defaultMockProjects_containsBakeryAndClinicProjects() {
        val projects = FreelanceViewModel.defaultMockProjects()

        assertEquals(2, projects.size)
        assertEquals("Bakery website", projects[0].name)
        assertEquals("Clinic booking site", projects[1].name)
        assertEquals(120_000L, projects[0].totalBudget)
        assertEquals(120_000L, projects[1].totalBudget)
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
