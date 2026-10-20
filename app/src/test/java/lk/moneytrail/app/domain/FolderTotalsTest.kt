package lk.moneytrail.app.domain

import lk.moneytrail.app.domain.model.CategoryTotal
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.SpendingType
import lk.moneytrail.app.domain.model.categoryTotals
import lk.moneytrail.app.domain.model.monthTotals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class FolderTotalsTest {

    private val october = YearMonth.of(2026, 10)

    private fun expense(amount: Long, category: String, date: LocalDate) =
        Entry("", EntryType.EXPENSE, amount, category, "Main bank", date, "", SpendingType.DISCRETIONARY)

    private val entries = listOf(
        expense(34_000, "Rent", LocalDate.of(2026, 10, 1)),
        expense(850, "Coffee", LocalDate.of(2026, 10, 2)),
        expense(2_100, "Coffee", LocalDate.of(2026, 10, 9)),
        expense(41_000, "Rent", LocalDate.of(2026, 9, 1)),
        Entry("", EntryType.INCOME, 120_000, "Salary", "Main bank", LocalDate.of(2026, 10, 25), "", null)
    )

    @Test
    fun categoryTotals_groupOneMonthOfOneTypeBiggestFirst() {
        val totals = entries.categoryTotals(EntryType.EXPENSE, october)

        assertEquals(
            listOf(CategoryTotal("Rent", 34_000, 1), CategoryTotal("Coffee", 2_950, 2)),
            totals
        )
    }

    @Test
    fun categoryTotals_withEqualTotalsAreSortedByName() {
        val day = LocalDate.of(2026, 10, 3)
        val totals = listOf(expense(500, "Rides", day), expense(500, "Coffee", day)).categoryTotals(EntryType.EXPENSE, october)

        assertEquals(listOf("Coffee", "Rides"), totals.map { it.category })
    }

    @Test
    fun monthTotals_areNewestFirstAndOnlyForTheType() {
        val totals = entries.monthTotals(EntryType.EXPENSE)

        assertEquals(listOf(october, YearMonth.of(2026, 9)), totals.map { it.month })
        assertEquals(36_950L, totals.first().total)
        assertEquals(3, totals.first().entryCount)
    }
}
