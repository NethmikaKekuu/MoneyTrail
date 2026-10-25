package lk.moneytrail.app.domain

import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryStatus
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.SpendingType
import lk.moneytrail.app.domain.model.categoryTotals
import lk.moneytrail.app.domain.model.confirmed
import lk.moneytrail.app.domain.model.confirmedSalary
import lk.moneytrail.app.domain.model.confirmedSalaryFromTotal
import lk.moneytrail.app.domain.model.dueExpected
import lk.moneytrail.app.domain.model.spendingSplit
import lk.moneytrail.app.domain.model.totals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ExpectedEntriesTest {

    private val today = LocalDate.of(2026, 10, 20)
    private val october = YearMonth.of(2026, 10)

    private fun income(id: String, amount: Long, date: LocalDate, status: EntryStatus) =
        Entry(id, EntryType.INCOME, amount, "Freelance", "Main bank", date, "", null, status)

    private fun bill(id: String, amount: Long, date: LocalDate, status: EntryStatus) =
        Entry(id, EntryType.EXPENSE, amount, "Utilities", "Main bank", date, "", SpendingType.COMMITTED, status)

    private val entries = listOf(
        income("paid", 45_000, LocalDate.of(2026, 10, 5), EntryStatus.CONFIRMED),
        income("overdue", 30_000, LocalDate.of(2026, 10, 14), EntryStatus.EXPECTED),
        income("later", 20_000, LocalDate.of(2026, 10, 28), EntryStatus.EXPECTED),
        bill("electricity", 7_400, today, EntryStatus.EXPECTED),
        bill("water", 1_200, LocalDate.of(2026, 10, 10), EntryStatus.CONFIRMED)
    )

    @Test
    fun totals_keepExpectedMoneyApart() {
        val totals = entries.totals()

        assertEquals(45_000L, totals.income)
        assertEquals(1_200L, totals.expenses)
        assertEquals(50_000L, totals.expectedIncome)
        assertEquals(7_400L, totals.expectedExpenses)
    }

    @Test
    fun dueExpected_isTodayAndOverdueOldestFirst() {
        val due = entries.dueExpected(today)

        assertEquals(listOf("overdue", "electricity"), due.map { it.id })
    }

    @Test
    fun confirmed_usesWhatReallyHappened() {
        val adSense = income("ads", 15_000, LocalDate.of(2026, 10, 14), EntryStatus.EXPECTED)

        val confirmed = adSense.confirmed(amount = 12_600, date = today)

        assertEquals(EntryStatus.CONFIRMED, confirmed.status)
        assertEquals(12_600L, confirmed.amount)
        assertEquals(today, confirmed.date)
        assertFalse(confirmed.isExpected)
    }

    @Test
    fun confirmedSalary_addsTheBonusToTheSalaryItself() {
        val salary = Entry("s1", EntryType.INCOME, 120_000, "Salary", "Main bank", LocalDate.of(2026, 10, 25), "", null, EntryStatus.EXPECTED)

        val confirmed = salary.confirmedSalary(base = 120_000, bonus = 15_000, date = today)

        assertEquals(135_000L, confirmed.amount)
        assertEquals(15_000L, confirmed.bonus)
        assertEquals(120_000L, confirmed.baseAmount)
        assertEquals(EntryStatus.CONFIRMED, confirmed.status)
    }

    @Test
    fun confirmedSalaryFromTotal_splitsAnythingAboveTheBaseIntoTheBonus() {
        val salary = Entry("s1", EntryType.INCOME, 120_000, "Salary", "Main bank", LocalDate.of(2026, 10, 25), "", null, EntryStatus.EXPECTED)

        val confirmed = salary.confirmedSalaryFromTotal(total = 132_400, base = 120_000, date = today)

        assertEquals(132_400L, confirmed.amount)
        assertEquals(12_400L, confirmed.bonus)
        assertEquals(120_000L, confirmed.baseAmount)
        assertEquals(EntryStatus.CONFIRMED, confirmed.status)
    }

    @Test
    fun confirmedSalaryFromTotal_savesWhatArrivedWhenItIsLessThanTheBase() {
        val salary = Entry("s1", EntryType.INCOME, 120_000, "Salary", "Main bank", LocalDate.of(2026, 10, 25), "", null, EntryStatus.EXPECTED)

        val confirmed = salary.confirmedSalaryFromTotal(total = 110_000, base = 120_000, date = today)

        assertEquals(110_000L, confirmed.amount)
        assertEquals(0L, confirmed.bonus)
    }

    @Test
    fun foldersAndInsights_ignoreExpectedEntries() {
        assertEquals(listOf(1_200L), entries.categoryTotals(EntryType.EXPENSE, october).map { it.total })
        assertEquals(1_200L, entries.spendingSplit(october).committed)
    }
}
