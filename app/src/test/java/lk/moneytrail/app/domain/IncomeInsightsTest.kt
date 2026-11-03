package lk.moneytrail.app.domain

import lk.moneytrail.app.domain.model.Categories
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryStatus
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.IncomeSource
import lk.moneytrail.app.domain.model.SpendingType
import lk.moneytrail.app.domain.model.cryptoTotal
import lk.moneytrail.app.domain.model.dependability
import lk.moneytrail.app.domain.model.incomeSource
import lk.moneytrail.app.domain.model.lastMonthsBySource
import lk.moneytrail.app.domain.model.monthIncome
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class IncomeInsightsTest {

    private val sep = YearMonth.of(2026, 9)
    private val oct = YearMonth.of(2026, 10)

    private fun income(amount: Long, category: String, date: LocalDate, bonus: Long = 0, expected: Boolean = false, usdCents: Long = 0) =
        Entry(
            id = "", type = EntryType.INCOME, amount = amount, category = category, account = "Main bank",
            date = date, note = "", spendingType = null,
            status = if (expected) EntryStatus.EXPECTED else EntryStatus.CONFIRMED,
            bonus = bonus, usdCents = usdCents
        )

    // Kavindu's September: salary with a bonus, one freelance milestone, AdSense, and a crypto gain.
    private val entries = listOf(
        income(133_400, Categories.SALARY, sep.atDay(25), bonus = 13_400),
        income(35_000, "Freelance", sep.atDay(12)),
        income(12_600, Categories.ADSENSE, sep.atDay(5), usdCents = 4_200_00),
        income(8_000, "Crypto", sep.atDay(20)),
        income(56_210, "Freelance", oct.atDay(3)),
        income(130_000, Categories.SALARY, oct.atDay(25), expected = true),
        Entry("", EntryType.EXPENSE, 34_000, "Rent", "Main bank", oct.atDay(1), "", SpendingType.COMMITTED)
    )

    @Test
    fun incomeSource_followsCategory() {
        assertEquals(IncomeSource.SALARY, entries[0].incomeSource)
        assertEquals(IncomeSource.FREELANCE, entries[1].incomeSource)
        assertEquals(IncomeSource.ADSENSE, entries[2].incomeSource)
        assertEquals(IncomeSource.CRYPTO, entries[3].incomeSource)
    }

    @Test
    fun monthIncome_keepsConfirmedAndExpectedApart() {
        val october = entries.monthIncome(oct)
        assertEquals(56_210L, october.confirmed)
        assertEquals(130_000L, october.expected)
    }

    @Test
    fun lastMonthsBySource_leavesCryptoOutAndKeepsEmptyMonths() {
        val months = entries.lastMonthsBySource(oct, 6)
        assertEquals(6, months.size)
        assertEquals(YearMonth.of(2026, 5), months.first().month)
        assertEquals(0L, months.first().total)

        val september = months[4]
        assertEquals(133_400L, september.amount(IncomeSource.SALARY))
        assertEquals(35_000L, september.amount(IncomeSource.FREELANCE))
        assertEquals(12_600L, september.amount(IncomeSource.ADSENSE))
        assertEquals(0L, september.amount(IncomeSource.CRYPTO))
        assertEquals(181_000L, september.total)

        // October's salary is only expected, so it isn't in the bar yet.
        assertEquals(56_210L, months.last().total)
    }

    @Test
    fun dependability_isTheBaseSalaryOnly() {
        val split = entries.dependability(sep)
        assertEquals(120_000L, split.dependable)
        assertEquals(61_000L, split.variable)
        assertEquals(66, split.dependablePercent)
        assertEquals(34, split.variablePercent)
    }

    @Test
    fun dependability_withNothingConfirmed_isZero() {
        val split = emptyList<Entry>().dependability(sep)
        assertEquals(0L, split.total)
        assertEquals(0, split.dependablePercent)
        assertEquals(0, split.variablePercent)
    }

    @Test
    fun cryptoTotal_countsThisYearOnly() {
        val lastYear = income(5_000, "Crypto", LocalDate.of(2025, 12, 30))
        assertEquals(8_000L, (entries + lastYear).cryptoTotal(2026))
    }
}
