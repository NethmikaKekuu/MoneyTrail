package lk.moneytrail.app.domain.model

/**
 * Money in and out over some set of entries, e.g. a month or a day. [income] and
 * [expenses] count only confirmed entries; expected ones are kept apart.
 */
data class EntryTotals(
    val income: Long,
    val expenses: Long,
    val expectedIncome: Long = 0,
    val expectedExpenses: Long = 0
) {
    /** Positive when more came in than went out. */
    val net: Long
        get() = income - expenses
}

fun List<Entry>.totals(): EntryTotals {
    fun sum(type: EntryType, expected: Boolean) =
        filter { it.type == type && it.isExpected == expected }.sumOf { it.amount }
    return EntryTotals(
        income = sum(EntryType.INCOME, expected = false),
        expenses = sum(EntryType.EXPENSE, expected = false),
        expectedIncome = sum(EntryType.INCOME, expected = true),
        expectedExpenses = sum(EntryType.EXPENSE, expected = true)
    )
}
