package lk.moneytrail.app.domain.model

import java.time.LocalDate
import java.time.YearMonth

/**
 * A bill that leaves the account on the same day every month, such as a subscription or
 * an auto-debit: the users/{uid}/recurringBills/{id} document in Firestore.
 */
data class RecurringBill(
    /** Firestore document id. Empty until the bill is first saved. */
    val id: String = "",
    val name: String,
    /** In whole rupees (LKR). The real amount is typed again when the bill is paid, e.g. for utilities. */
    val amount: Long,
    /** Name of the account it is debited from. */
    val account: String,
    /** An expense category, built-in or the user's own. */
    val category: String,
    /** Day of the month it falls due, 1 to 31. A 31 falls on the last day of shorter months. */
    val dueDay: Int,
    /** A stopped bill makes no new entries; the ones it already made stay. */
    val isActive: Boolean = true,
    /** The latest month this bill has been put on the calendar for; null before the first time. */
    val lastCreatedMonth: YearMonth? = null
)

/** The day this bill falls due in [month], never later than the month's last day. */
fun RecurringBill.dueDateIn(month: YearMonth): LocalDate =
    month.atDay(dueDay.coerceAtMost(month.lengthOfMonth()))

/**
 * The months, oldest first, this bill still has to be put on the calendar for as of
 * [today]: after [RecurringBill.lastCreatedMonth] up to this month, but no more than
 * [MAX_MONTHS_BACK] months back so a long break doesn't flood the calendar. A bill that
 * is stopped, or already done for this month, has none.
 */
fun RecurringBill.monthsToCreate(today: LocalDate): List<YearMonth> {
    if (!isActive) return emptyList()
    val thisMonth = YearMonth.from(today)
    val earliest = thisMonth.minusMonths(RecurringBills.MAX_MONTHS_BACK.toLong())
    val first = lastCreatedMonth?.plusMonths(1)?.let { maxOf(it, earliest) } ?: thisMonth
    return generateSequence(first) { it.plusMonths(1) }.takeWhile { !it.isAfter(thisMonth) }.toList()
}

/**
 * What the bill looks like on the calendar for [month]: an expected expense on its due
 * date, to be checked with "Did you pay it?". A bill is committed spending whatever its
 * category, as it leaves the account whether the user likes it or not.
 */
fun RecurringBill.toExpectedEntry(month: YearMonth): Entry = Entry(
    id = "",
    type = EntryType.EXPENSE,
    amount = amount,
    category = category,
    account = account,
    date = dueDateIn(month),
    note = name,
    spendingType = SpendingType.COMMITTED,
    status = EntryStatus.EXPECTED
)

object RecurringBills {
    /** How many past months are put on the calendar when the app hasn't run for a while. */
    const val MAX_MONTHS_BACK = 2

    private const val MAX_AMOUNT = 999_999_999L

    /** Why [bill] can't be saved, in words for the user; null when it's fine. Mirrors firestore.rules. */
    fun billError(bill: RecurringBill): String? = when {
        bill.name.isBlank() -> "Enter a name for the bill."
        bill.name.trim().length > 60 -> "Keep the name to 60 characters."
        bill.amount <= 0 -> "Enter the amount."
        bill.amount > MAX_AMOUNT -> "That amount is too large."
        bill.account.isBlank() -> "Pick the account it is paid from."
        bill.category.isBlank() -> "Pick a category."
        bill.dueDay !in 1..31 -> "The due day must be from 1 to 31."
        else -> null
    }
}
