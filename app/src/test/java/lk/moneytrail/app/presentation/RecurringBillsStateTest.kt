package lk.moneytrail.app.presentation

import lk.moneytrail.app.domain.model.RecurringBill
import lk.moneytrail.app.presentation.bills.RecurringBillsState
import lk.moneytrail.app.presentation.bills.dueDayText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RecurringBillsStateTest {

    private val today = LocalDate.of(2026, 11, 5)
    private fun bill(id: String, day: Int, active: Boolean = true, amount: Long = 1_000) =
        RecurringBill(id, "Bill $id", amount, "Main bank", "Subscriptions", day, active)

    private val state = RecurringBillsState(
        isLoading = false,
        bills = listOf(bill("a", 3), bill("b", 20, amount = 2_000), bill("c", 1, active = false), bill("d", 5))
    )

    @Test
    fun totalsCountOnlyActiveBills() {
        assertEquals(4_000L, state.monthlyTotal)
        assertEquals(3, state.activeCount)
        assertEquals(1, state.stoppedCount)
    }

    @Test
    fun activeBillsFirst_inDueDayOrder() {
        assertEquals(listOf("a", "d", "b", "c"), state.sortedBills.map { it.id })
    }

    @Test
    fun nextDue_isTodayOrLater_andWrapsToNextMonth() {
        assertEquals("d" to today, state.nextDue(today)!!.let { it.first.id to it.second })
        val onlyEarly = RecurringBillsState(bills = listOf(bill("a", 3)))
        assertEquals(LocalDate.of(2026, 12, 3), onlyEarly.nextDue(today)!!.second)
        assertNull(RecurringBillsState(bills = listOf(bill("c", 1, active = false))).nextDue(today))
    }

    @Test
    fun dueDayText_usesTheRightSuffix() {
        assertEquals("Due on the 1st every month", bill("x", 1).dueDayText())
        assertEquals("Due on the 2nd every month", bill("x", 2).dueDayText())
        assertEquals("Due on the 11th every month", bill("x", 11).dueDayText())
        assertEquals("Due on the 23rd every month", bill("x", 23).dueDayText())
    }
}
