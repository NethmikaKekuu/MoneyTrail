package lk.moneytrail.app.presentation

import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.SpendingType
import lk.moneytrail.app.presentation.calendar.buildCalendarDays
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalendarDaysTest {

    private val october = YearMonth.of(2026, 10)

    @Test
    fun grid_isWholeMondayToSundayWeeks() {
        val days = buildCalendarDays(october, emptyList())

        // 1 October 2026 is a Thursday and the 31st a Saturday.
        assertEquals(LocalDate.of(2026, 9, 28), days.first().date)
        assertEquals(LocalDate.of(2026, 11, 1), days.last().date)
        assertEquals(35, days.size)
        assertFalse(days.first().isInMonth)
        assertTrue(days.single { it.date == LocalDate.of(2026, 10, 1) }.isInMonth)
    }

    @Test
    fun days_markIncomeAndExpenseSeparately() {
        val salaryDay = LocalDate.of(2026, 10, 25)
        val busyDay = LocalDate.of(2026, 10, 10)
        val entries = listOf(
            Entry("1", EntryType.INCOME, 120_000, "Salary", "Main bank", salaryDay, "", null),
            Entry("2", EntryType.EXPENSE, 850, "Coffee", "Cash wallet", busyDay, "", SpendingType.DISCRETIONARY),
            Entry("3", EntryType.EXPENSE, 780, "Rides", "Cash wallet", busyDay, "", SpendingType.DISCRETIONARY)
        )

        val days = buildCalendarDays(october, entries).associateBy { it.date }

        assertTrue(days.getValue(salaryDay).hasIncome)
        assertFalse(days.getValue(salaryDay).hasExpense)
        assertTrue(days.getValue(busyDay).hasExpense)
        assertEquals(2, days.getValue(busyDay).entryCount)
        assertEquals(0, days.getValue(LocalDate.of(2026, 10, 11)).entryCount)
    }

    @Test
    fun monthStartingOnMonday_hasNoDaysFromTheMonthBefore() {
        // 1 June 2026 is a Monday.
        val days = buildCalendarDays(YearMonth.of(2026, 6), emptyList())

        assertEquals(LocalDate.of(2026, 6, 1), days.first().date)
    }
}
