package lk.moneytrail.app.domain

import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.SavingsGoal
import lk.moneytrail.app.domain.model.SpendingSplit
import lk.moneytrail.app.domain.model.SpendingType
import lk.moneytrail.app.domain.model.daysOfSaving
import lk.moneytrail.app.domain.model.diningSpend
import lk.moneytrail.app.domain.model.lastMonthsTotals
import lk.moneytrail.app.domain.model.spendingSplit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class SpendingInsightsTest {

    private val october = YearMonth.of(2026, 10)
    private val today = LocalDate.of(2026, 10, 19)

    private fun expense(amount: Long, category: String, type: SpendingType, date: LocalDate = today) =
        Entry("", EntryType.EXPENSE, amount, category, "Main bank", date, "", type)

    private val entries = listOf(
        expense(34_000, "Rent", SpendingType.COMMITTED),
        expense(7_400, "Utilities", SpendingType.COMMITTED),
        expense(2_950, "Coffee", SpendingType.DISCRETIONARY),
        expense(2_400, "Food out", SpendingType.DISCRETIONARY),
        expense(2_300, "Food delivery", SpendingType.DISCRETIONARY),
        expense(1_730, "Rides", SpendingType.DISCRETIONARY),
        expense(9_999, "Coffee", SpendingType.DISCRETIONARY, LocalDate.of(2026, 9, 30)),
        Entry("", EntryType.INCOME, 120_000, "Salary", "Main bank", today, "", null)
    )

    @Test
    fun split_dividesThisMonthsExpensesOnly() {
        val split = entries.spendingSplit(october)

        assertEquals(41_400L, split.committed)
        assertEquals(9_380L, split.discretionary)
        assertEquals(82, split.committedPercent)
        assertEquals(18, split.discretionaryPercent)
    }

    @Test
    fun split_ofNothingIsZeroPercent() {
        val split = SpendingSplit(committed = 0, discretionary = 0)

        assertEquals(0, split.committedPercent)
        assertEquals(0, split.discretionaryPercent)
    }

    @Test
    fun diningSpend_countsCoffeeAndDiningOutThisMonth() {
        assertEquals(7_650L, entries.diningSpend(october))
    }

    @Test
    fun lastMonthsTotals_includeEmptyMonthsOldestFirst() {
        val months = entries.lastMonthsTotals(EntryType.EXPENSE, october, 6)

        assertEquals(YearMonth.of(2026, 5), months.first().month)
        assertEquals(october, months.last().month)
        assertEquals(6, months.size)
        assertEquals(0L, months.first().total)
        assertEquals(9_999L, months[4].total)
    }

    @Test
    fun daysOfSaving_forKavindu() {
        val macBook = SavingsGoal("MacBook Pro M4", 490_000, 11_200, LocalDate.of(2027, 10, 19))

        // 39,900 a month is about 1,330 a day, so 7,650 is about 6 days.
        assertEquals(6, macBook.daysOfSaving(7_650, today))
        assertEquals(1, macBook.daysOfSaving(100, today))
    }

    @Test
    fun daysOfSaving_isNullOnceTheGoalIsReached() {
        val done = SavingsGoal("Phone", 100_000, 100_000, LocalDate.of(2027, 1, 1))

        assertNull(done.daysOfSaving(7_650, today))
    }
}
