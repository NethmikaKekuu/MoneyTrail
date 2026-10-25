package lk.moneytrail.app.domain.model

import java.time.YearMonth

/** One category inside a folder, e.g. Rent in Expenses › October. */
data class CategoryTotal(
    val category: String,
    val total: Long,
    val entryCount: Int
)

/** All entries of one type in one month, e.g. Expenses › September. */
data class MonthTotal(
    val month: YearMonth,
    val total: Long,
    val entryCount: Int
)

/**
 * The confirmed entries of [type] dated in [month], grouped by category, biggest total
 * first. Equal totals are sorted by name so the order doesn't jump around.
 */
fun List<Entry>.categoryTotals(type: EntryType, month: YearMonth): List<CategoryTotal> =
    filter { it.type == type && !it.isExpected && YearMonth.from(it.date) == month }
        .groupBy { it.category }
        .map { (category, entries) -> CategoryTotal(category, entries.sumOf { it.amount }, entries.size) }
        .sortedWith(compareByDescending<CategoryTotal> { it.total }.thenBy { it.category })

/** The confirmed total of [type] for every month that has some, newest month first. */
fun List<Entry>.monthTotals(type: EntryType): List<MonthTotal> =
    filter { it.type == type && !it.isExpected }
        .groupBy { YearMonth.from(it.date) }
        .map { (month, entries) -> MonthTotal(month, entries.sumOf { it.amount }, entries.size) }
        .sortedByDescending { it.month }
