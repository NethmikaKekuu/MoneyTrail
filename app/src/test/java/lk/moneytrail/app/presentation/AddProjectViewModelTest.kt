package lk.moneytrail.app.presentation

import lk.moneytrail.app.domain.model.Milestone
import lk.moneytrail.app.domain.model.MilestoneStatus
import lk.moneytrail.app.domain.model.dueDate
import lk.moneytrail.app.domain.model.dueTimestampOf
import lk.moneytrail.app.presentation.freelance.AddProjectUiState
import lk.moneytrail.app.presentation.freelance.buildNewProject
import lk.moneytrail.app.presentation.freelance.milestoneError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AddProjectViewModelTest {

    private val today = LocalDate.of(2026, 11, 5)
    private fun form(price: String, vararg amounts: Long) = AddProjectUiState(
        projectName = "Salon website",
        clientName = "Glow Salon",
        totalBudgetDigits = price,
        milestones = amounts.mapIndexed { i, a -> Milestone("m$i", "Part $i", a, MilestoneStatus.PLANNED, dueTimestampOf(today.plusWeeks(i + 1L))) }
    )

    @Test
    fun price_isOnlyWhatWasTyped_neverTheMilestoneTotal() {
        assertEquals(0L, form("", 20_000L, 25_000L).totalProjectPrice)
        assertEquals(60_000L, form("60000", 20_000L).totalProjectPrice)
    }

    @Test
    fun splitText_showsWhatIsLeft() {
        assertNull(form("").splitText)
        assertEquals("Split 40,000 of 60,000 · 20,000 left", form("60000", 15_000L, 25_000L).splitText)
        assertEquals("Fully split: 60,000 of 60,000", form("60000", 60_000L).splitText)
        assertEquals("Milestones are 5,000 over the price", form("60000", 65_000L).splitText)
    }

    @Test
    fun saveError_needsNameClientAndPrice_andNoOverSplit() {
        assertEquals("Enter a project name.", form("60000").copy(projectName = " ").saveError)
        assertEquals("Enter the client name.", form("60000").copy(clientName = "").saveError)
        assertEquals("Enter the total project price.", form("").saveError)
        assertTrue(form("60000", 70_000L).saveError!!.startsWith("Milestones add up to 70,000"))
        assertNull(form("60000").saveError)
        assertTrue(form("60000", 20_000L).isValid)
    }

    @Test
    fun noMilestones_savesTheWholePriceAsOnePayment() {
        val project = buildNewProject(form("80000"), today, "p1")
        assertEquals(80_000L, project.totalBudget)
        assertEquals(1, project.milestones.size)
        assertEquals("Full payment", project.milestones[0].title)
        assertEquals(80_000L, project.milestones[0].amount)
        assertEquals(today.plusWeeks(2), project.milestones[0].dueDate)
    }

    @Test
    fun withMilestones_keepsThemAndTheTypedPrice() {
        val project = buildNewProject(form("60000", 20_000L, 40_000L), today, "p1")
        assertEquals(60_000L, project.totalBudget)
        assertEquals(listOf(20_000L, 40_000L), project.milestones.map { it.amount })
    }

    @Test
    fun nextDueDate_isTwoWeeksAfterTheLatestMilestone() {
        assertEquals(today.plusWeeks(2), form("60000").nextDueDate(today))
        assertEquals(today.plusWeeks(2 + 2), form("60000", 1L, 1L).nextDueDate(today))
    }

    @Test
    fun milestoneError_checksTitleAmountAndWhatIsLeft() {
        assertEquals("Enter a milestone title.", milestoneError(" ", 100, null))
        assertEquals("Enter an amount greater than 0.", milestoneError("Design", 0, null))
        assertEquals("Only 20,000 of the price is left to split.", milestoneError("Design", 25_000, 20_000))
        assertEquals("The whole price is already split into milestones.", milestoneError("Design", 1, 0))
        assertNull(milestoneError("Design", 20_000, 20_000))
    }

    @Test
    fun newProject_startsBlank() {
        val state = AddProjectUiState()
        assertTrue(state.milestones.isEmpty())
        assertFalse(state.isValid)
    }
}
