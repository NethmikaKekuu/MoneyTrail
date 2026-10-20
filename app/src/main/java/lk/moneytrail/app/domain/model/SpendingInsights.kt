package lk.moneytrail.app.domain.model

import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToLong

/** How a month's spending divides between what had to be paid and what was a choice. */
data class SpendingSplit(
    val committed: Long,
    val discretionary: Long
) {
    val total: Long
        get() = committed + discretionary

    /** Committed share of the total, 0 to 100; the two percentages always add up to 100. */
    val committedPercent: Int
        get() = if (total == 0L) 0 else (committed * 100.0 / total).roundToLong().toInt()

    val discretionaryPercent: Int
        get() = if (total == 0L) 0 else 100 - committedPercent
}

/** The categories counted as "coffee and dining out", Kavindu's LKR 24,000 surprise. */
val DiningCategories: Set<String> = setOf("Coffee", "Food out", "Food delivery")

/** Committed and discretionary spending in [month]. An expense with no type counts as discretionary. */
fun List<Entry>.spendingSplit(month: YearMonth): SpendingSplit {
    val expenses = filter { it.type == EntryType.EXPENSE && YearMonth.from(it.date) == month }
    return SpendingSplit(
        committed = expenses.filter { it.spendingType == SpendingType.COMMITTED }.sumOf { it.amount },
        discretionary = expenses.filter { it.spendingType != SpendingType.COMMITTED }.sumOf { it.amount }
    )
}

/** What went on coffee and dining out in [month]. */
fun List<Entry>.diningSpend(month: YearMonth): Long =
    filter { it.type == EntryType.EXPENSE && it.category in DiningCategories && YearMonth.from(it.date) == month }
        .sumOf { it.amount }

/**
 * The totals of [type] for the [count] months up to and including [lastMonth], oldest
 * first. Months with no entries are included as zero, so a chart always has every bar.
 */
fun List<Entry>.lastMonthsTotals(type: EntryType, lastMonth: YearMonth, count: Int): List<MonthTotal> {
    val totals = monthTotals(type).associateBy { it.month }
    return (count - 1 downTo 0).map { back ->
        val month = lastMonth.minusMonths(back.toLong())
        totals[month] ?: MonthTotal(month, 0, 0)
    }
}

/**
 * How many days of saving towards the goal [amount] is worth, at the pace needed to
 * reach it on time (a month counted as 30 days). At least 1 for any amount spent; null
 * when there's nothing to compare, e.g. the goal is already reached.
 *
 * Kavindu needs 39,900 a month, about 1,330 a day, so LKR 7,650 of coffee and dining
 * out is about 6 days.
 */
fun SavingsGoal.daysOfSaving(amount: Long, today: LocalDate): Int? {
    val monthly = monthlySavingNeeded(today)
    if (monthly <= 0 || amount <= 0) return null
    val perDay = monthly / 30.0
    return (amount / perDay).roundToLong().toInt().coerceAtLeast(1)
}
