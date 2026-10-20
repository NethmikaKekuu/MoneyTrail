package lk.moneytrail.app.presentation.folders

import lk.moneytrail.app.domain.model.CategoryTotal
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.MonthTotal
import lk.moneytrail.app.domain.model.categoryTotals
import lk.moneytrail.app.domain.model.monthTotals
import java.time.YearMonth

data class FoldersState(
    val currentMonth: YearMonth,
    /** The month whose folders are open; the current one to begin with. */
    val selectedMonth: YearMonth = currentMonth,
    /** Expenses or Income, whichever folder card is picked. */
    val selectedType: EntryType = EntryType.EXPENSE,
    /** True until the first entries arrive. */
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    /** Every entry from the oldest month listed up to the end of the current month. */
    val entries: List<Entry> = emptyList()
) {
    val expenses: MonthTotal
        get() = totalFor(EntryType.EXPENSE)

    val income: MonthTotal
        get() = totalFor(EntryType.INCOME)

    /** The open folder's categories, biggest first. */
    val categories: List<CategoryTotal>
        get() = entries.categoryTotals(selectedType, selectedMonth)

    /**
     * The other months to switch to, newest first: every month with entries in the open
     * folder, plus the current month so there's always a way back to it.
     */
    val otherMonths: List<MonthTotal>
        get() {
            val withEntries = entries.monthTotals(selectedType)
            val current = withEntries.find { it.month == currentMonth } ?: MonthTotal(currentMonth, 0, 0)
            return (withEntries + current)
                .distinctBy { it.month }
                .filter { it.month != selectedMonth }
                .sortedByDescending { it.month }
        }

    private fun totalFor(type: EntryType): MonthTotal =
        entries.monthTotals(type).find { it.month == selectedMonth } ?: MonthTotal(selectedMonth, 0, 0)
}
