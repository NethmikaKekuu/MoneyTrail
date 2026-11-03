package lk.moneytrail.app.presentation

import lk.moneytrail.app.domain.model.Milestone
import lk.moneytrail.app.domain.model.MilestoneStatus
import lk.moneytrail.app.domain.model.Project
import lk.moneytrail.app.presentation.freelance.RecordFreelancePaymentUiState
import lk.moneytrail.app.presentation.freelance.UnpaidMilestoneOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RecordFreelancePaymentViewModelTest {

    @Test
    fun recordFreelancePaymentUiState_computesBannerMessageForLinkedMilestone() {
        val m1 = Milestone("m1", "Launch", 30_000L, MilestoneStatus.INVOICED)
        val p1 = Project("p1", "Bakery website", "React build", 120_000L, listOf(m1))
        val option = UnpaidMilestoneOption("m1", m1, p1, "Launch", 30_000L, "Bakery website · invoiced, due 14 Oct")

        val state = RecordFreelancePaymentUiState(
            amountDigits = "30000",
            selectedOptionId = "m1",
            milestoneOptions = listOf(option),
            selectedAccount = "Main bank",
            selectedDate = LocalDate.of(2026, 10, 14)
        )

        assertEquals(30_000L, state.amountValue)
        assertNotNull(state.selectedOption)
        assertEquals("Launch", state.selectedOption?.milestone?.title)
        assertEquals("Wed, 14 Oct", state.formattedDateText)
        assertEquals("Launch will be marked Paid and outstanding drops by 30,000.", state.infoBannerMessage)
    }

    @Test
    fun recordFreelancePaymentUiState_computesBannerMessageForUnlinkedIncome() {
        val option = UnpaidMilestoneOption("NO_MILESTONE", null, null, "No milestone", 0L, "Save as unlinked freelance income")

        val state = RecordFreelancePaymentUiState(
            amountDigits = "20000",
            selectedOptionId = "NO_MILESTONE",
            milestoneOptions = listOf(option),
            selectedAccount = "Savings",
            selectedDate = LocalDate.of(2026, 10, 20)
        )

        assertEquals(20_000L, state.amountValue)
        assertNull(state.selectedOption?.milestone)
        assertEquals("Saved as unlinked freelance income in Savings.", state.infoBannerMessage)
    }
}
