package lk.moneytrail.app.presentation.calendar

import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** One square of the month grid. */
data class CalendarDay(
    val date: LocalDate,
    /** False for the days of the previous or next month that fill the first and last week. */
    val isInMonth: Boolean,
    val hasIncome: Boolean,
    val hasExpense: Boolean,
    val entryCount: Int
)

/**
 * [month] as whole Monday-to-Sunday weeks, with markers for the days that have entries.
 * October 2026 starts on a Thursday, so its grid starts on Monday 28 September.
 */
fun buildCalendarDays(month: YearMonth, entries: List<Entry>): List<CalendarDay> {
    val entriesByDate = entries.groupBy { it.date }
    val firstDay = month.atDay(1)
    val lastDay = month.atEndOfMonth()
    val gridStart = firstDay.minusDays((firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
    val gridEnd = lastDay.plusDays((DayOfWeek.SUNDAY.value - lastDay.dayOfWeek.value).toLong())

    return generateSequence(gridStart) { it.plusDays(1) }
        .takeWhile { !it.isAfter(gridEnd) }
        .map { date ->
            val isInMonth = YearMonth.from(date) == month
            val dayEntries = if (isInMonth) entriesByDate[date].orEmpty() else emptyList()
            CalendarDay(
                date = date,
                isInMonth = isInMonth,
                hasIncome = dayEntries.any { it.type == EntryType.INCOME },
                hasExpense = dayEntries.any { it.type == EntryType.EXPENSE },
                entryCount = dayEntries.size
            )
        }
        .toList()
}
