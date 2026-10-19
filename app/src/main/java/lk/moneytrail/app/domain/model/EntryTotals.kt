package lk.moneytrail.app.domain.model

/** Money in and out over some set of entries, e.g. a month or a day. */
data class EntryTotals(
    val income: Long,
    val expenses: Long
) {
    /** Positive when more came in than went out. */
    val net: Long
        get() = income - expenses
}

fun List<Entry>.totals(): EntryTotals = EntryTotals(
    income = filter { it.type == EntryType.INCOME }.sumOf { it.amount },
    expenses = filter { it.type == EntryType.EXPENSE }.sumOf { it.amount }
)
