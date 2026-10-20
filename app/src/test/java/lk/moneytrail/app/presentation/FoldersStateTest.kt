package lk.moneytrail.app.presentation

import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.SpendingType
import lk.moneytrail.app.presentation.folders.FoldersState
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class FoldersStateTest {

    private val october = YearMonth.of(2026, 10)
    private val september = YearMonth.of(2026, 9)

    private val septemberRent = Entry(
        "1", EntryType.EXPENSE, 41_000, "Rent", "Main bank", LocalDate.of(2026, 9, 1), "", SpendingType.COMMITTED
    )

    @Test
    fun currentMonthWithNoEntries_showsZeroTotals() {
        val state = FoldersState(currentMonth = october, entries = listOf(septemberRent))

        assertEquals(0L, state.expenses.total)
        assertEquals(0, state.expenses.entryCount)
        assertEquals(emptyList<Any>(), state.categories)
    }

    @Test
    fun otherMonths_listEarlierMonthsWithEntries() {
        val state = FoldersState(currentMonth = october, entries = listOf(septemberRent))

        assertEquals(listOf(september), state.otherMonths.map { it.month })
    }

    @Test
    fun otherMonths_alwaysOfferAWayBackToTheCurrentMonth() {
        // September is open and October has no entries yet: October is still listed.
        val state = FoldersState(currentMonth = october, selectedMonth = september, entries = listOf(septemberRent))

        assertEquals(listOf(october), state.otherMonths.map { it.month })
        assertEquals(41_000L, state.expenses.total)
    }
}
