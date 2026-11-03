package lk.moneytrail.app.presentation

import lk.moneytrail.app.domain.model.Milestone
import lk.moneytrail.app.domain.model.MilestoneStatus
import lk.moneytrail.app.domain.model.Project
import lk.moneytrail.app.presentation.freelance.dueText
import lk.moneytrail.app.presentation.freelance.nextUnpaidMilestone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class RecordFreelancePaymentTest {

    private val today = LocalDate.of(2026, 11, 3)

    private fun millis(date: LocalDate) = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun nextUnpaidMilestone_prefersInvoicedOverPlanned() {
        val project = Project(
            "p1", "Site", "Client", 60_000L,
            listOf(
                Milestone("a", "Design", 20_000L, MilestoneStatus.PAID),
                Milestone("b", "Build", 25_000L, MilestoneStatus.PLANNED, millis(today.plusDays(2))),
                Milestone("c", "Launch", 15_000L, MilestoneStatus.INVOICED, millis(today.plusDays(9)))
            )
        )
        assertEquals("c", project.nextUnpaidMilestone()?.id)
    }

    @Test
    fun nextUnpaidMilestone_isNullWhenAllPaid() {
        val project = Project("p1", "Site", "Client", 20_000L, listOf(Milestone("a", "Design", 20_000L, MilestoneStatus.PAID)))
        assertNull(project.nextUnpaidMilestone())
    }

    @Test
    fun dueText_usesTheRealDueDate() {
        val invoiced = Milestone("a", "Launch", 1L, MilestoneStatus.INVOICED, millis(LocalDate.of(2026, 11, 14)))
        assertEquals("invoiced, due 14 Nov", invoiced.dueText(today))
        assertEquals("invoiced, due today", invoiced.copy(dueTimestamp = millis(today)).dueText(today))
        val late = Milestone("b", "Build", 1L, MilestoneStatus.PLANNED, millis(today.minusDays(3)))
        assertEquals("planned, overdue 3 days", late.dueText(today))
        assertEquals("planned", Milestone("c", "Extra", 1L).dueText(today))
    }
}
