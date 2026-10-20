package lk.moneytrail.app.presentation.calendar

import lk.moneytrail.app.domain.model.Categories
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryType
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
    val entries: List<Entry> = emptyList(),
    /** Expected entries whose day has come, from any month, oldest first. */
    val dueExpected: List<Entry> = emptyList(),
    /** The expected entry being checked, while its dialog is open. */
    val check: ExpectedCheck? = null
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

/** "Did it come in?" for one expected entry, with what the user has typed so far. */
data class ExpectedCheck(
    val entry: Entry,
    /** What really arrived or was paid; starts as the expected amount. */
    val amountDigits: String,
    /** When the money really moved: the planned day or today. */
    val date: LocalDate,
    /** For a salary: this month's bonus, saved as its own Bonus entry. Empty for none. */
    val bonusDigits: String = "",
    /** True while the "Not yet" date picker is open. */
    val isPickingNewDate: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
) {
    val isSalary: Boolean
        get() = entry.type == EntryType.INCOME && entry.category == Categories.SALARY
}
