package lk.moneytrail.app.domain

import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.SpendingType
import lk.moneytrail.app.domain.model.totals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class EntryTotalsTest {

    private val day = LocalDate.of(2026, 10, 10)

    private fun entry(type: EntryType, amount: Long) = Entry(
        id = "",
        type = type,
        amount = amount,
        category = "Other",
        account = "Main bank",
        date = day,
        note = "",
        spendingType = if (type == EntryType.EXPENSE) SpendingType.DISCRETIONARY else null
    )

    @Test
    fun totals_addUpIncomeAndExpensesSeparately() {
        val totals = listOf(
            entry(EntryType.INCOME, 120_000),
            entry(EntryType.EXPENSE, 850),
            entry(EntryType.EXPENSE, 2_400),
            entry(EntryType.INCOME, 12_600)
        ).totals()

        assertEquals(132_600L, totals.income)
        assertEquals(3_250L, totals.expenses)
        assertEquals(129_350L, totals.net)
    }

    @Test
    fun net_isNegativeWhenMoreWentOut() {
        val totals = listOf(entry(EntryType.EXPENSE, 4_030)).totals()

        assertEquals(-4_030L, totals.net)
    }

    @Test
    fun totals_ofNoEntriesAreZero() {
        val totals = emptyList<Entry>().totals()

        assertEquals(0L, totals.income)
        assertEquals(0L, totals.expenses)
        assertEquals(0L, totals.net)
    }
}
