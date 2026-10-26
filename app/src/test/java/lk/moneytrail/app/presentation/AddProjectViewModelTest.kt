package lk.moneytrail.app.presentation

import lk.moneytrail.app.domain.model.Milestone
import lk.moneytrail.app.domain.model.MilestoneStatus
import lk.moneytrail.app.presentation.freelance.AddProjectUiState
import lk.moneytrail.app.presentation.freelance.AddProjectViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddProjectViewModelTest {

    @Test
    fun addProjectUiState_calculatesTotalProjectPriceAndValidations() {
        val milestones = listOf(
            Milestone("1", "Design", 20_000L, MilestoneStatus.PLANNED),
            Milestone("2", "Build", 25_000L, MilestoneStatus.PLANNED),
            Milestone("3", "Launch", 15_000L, MilestoneStatus.PLANNED)
        )

        val state = AddProjectUiState(
            projectName = "Salon website",
            clientName = "Glow Salon, Kandy",
            milestones = milestones
        )

        assertEquals(60_000L, state.totalProjectPrice)
        assertEquals("3 added", state.milestoneCountText)
        assertTrue(state.isValid)
    }

    @Test
    fun addProjectUiState_withBlankFields_isValidReturnsFalse() {
        val invalidNameState = AddProjectUiState(
            projectName = "",
            clientName = "Glow Salon",
            milestones = AddProjectViewModel.defaultInitialMilestones()
        )
        assertFalse(invalidNameState.isValid)

        val invalidMilestonesState = AddProjectUiState(
            projectName = "Salon website",
            clientName = "Glow Salon",
            milestones = emptyList()
        )
        assertFalse(invalidMilestonesState.isValid)
    }

    @Test
    fun defaultInitialMilestones_containsDesignBuildLaunch() {
        val milestones = AddProjectViewModel.defaultInitialMilestones()

        assertEquals(3, milestones.size)
        assertEquals("Design", milestones[0].title)
        assertEquals(20_000L, milestones[0].amount)
        assertEquals("Build", milestones[1].title)
        assertEquals(25_000L, milestones[1].amount)
        assertEquals("Launch", milestones[2].title)
        assertEquals(15_000L, milestones[2].amount)
    }
}
