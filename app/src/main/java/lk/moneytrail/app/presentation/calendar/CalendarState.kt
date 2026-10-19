package lk.moneytrail.app.presentation.calendar

import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryTotals
import lk.moneytrail.app.domain.model.totals
import java.time.LocalDate
import java.time.YearMonth

data class CalendarState(
    /** For the "Hi Kavindu" greeting; null until the profile loads, or if it has no name. */
    val firstName: String? = null,
    val month: YearMonth,
    val today: LocalDate,
    val selectedDate: LocalDate,
    /** True until the first entries for [month] arrive. */
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    /** Every entry dated in [month], oldest first. */
    val entries: List<Entry> = emptyList()
) {
    val days: List<CalendarDay>
        get() = buildCalendarDays(month, entries)

    val monthTotals: EntryTotals
        get() = entries.totals()

    val selectedDayEntries: List<Entry>
        get() = entries.filter { it.date == selectedDate }

    val selectedDayTotals: EntryTotals
        get() = selectedDayEntries.totals()
}
