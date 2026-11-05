package lk.moneytrail.app.presentation.bills

import lk.moneytrail.app.domain.model.RecurringBill
import lk.moneytrail.app.domain.model.dueDateIn
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/** The bill being added or edited in the dialog, with the amount and day as typed. */
data class BillDraft(
    /** Empty for a new bill. */
    val id: String = "",
    val name: String = "",
    val amountDigits: String = "",
    val account: String = "",
    val category: String = "",
    val dueDayDigits: String = "",
    val isActive: Boolean = true,
    /** Kept from the saved bill so editing never makes a month's entry come back. */
    val lastCreatedMonth: YearMonth? = null,
    val errorMessage: String? = null
) {
    val isNew: Boolean
        get() = id.isEmpty()

    fun toBill(): RecurringBill = RecurringBill(
        id = id,
        name = name,
        amount = amountDigits.toLongOrNull() ?: 0L,
        account = account,
        category = category,
        dueDay = dueDayDigits.toIntOrNull() ?: 0,
        isActive = isActive,
        lastCreatedMonth = lastCreatedMonth
    )

    companion object {
        fun of(bill: RecurringBill) = BillDraft(
            id = bill.id,
            name = bill.name,
            amountDigits = bill.amount.toString(),
            account = bill.account,
            category = bill.category,
            dueDayDigits = bill.dueDay.toString(),
            isActive = bill.isActive,
            lastCreatedMonth = bill.lastCreatedMonth
        )
    }
}

data class RecurringBillsState(
    val isLoading: Boolean = true,
    /** Set when the bills can't be read; the screen then offers Try again. */
    val loadError: String? = null,
    val bills: List<RecurringBill> = emptyList(),
    /** Accounts to pick from: the user's own, or the defaults before onboarding is done. */
    val accountNames: List<String> = emptyList(),
    /** Expense categories to pick from: built-in first, then the user's own. */
    val categoryNames: List<String> = emptyList(),
    /** Not null while the add/edit dialog is open. */
    val draft: BillDraft? = null,
    val isSaving: Boolean = false,
    /** The bill waiting for "Delete this bill?" to be answered. */
    val deleting: RecurringBill? = null,
    /** One-off message such as a failed switch or delete, shown once. */
    val message: String? = null
) {
    /** What all active bills add up to each month, for the summary at the top. */
    val monthlyTotal: Long
        get() = bills.filter { it.isActive }.sumOf { it.amount }

    val activeCount: Int
        get() = bills.count { it.isActive }

    val stoppedCount: Int
        get() = bills.size - activeCount

    /** Active bills first, each group in due-day order. */
    val sortedBills: List<RecurringBill>
        get() = bills.sortedWith(compareByDescending<RecurringBill> { it.isActive }.thenBy { it.dueDay }.thenBy { it.name.lowercase(Locale.ENGLISH) })

    /** The next active bill to fall due on or after [today], and when. */
    fun nextDue(today: LocalDate): Pair<RecurringBill, LocalDate>? =
        bills.filter { it.isActive }
            .map { bill ->
                val thisMonth = bill.dueDateIn(YearMonth.from(today))
                bill to if (thisMonth.isBefore(today)) bill.dueDateIn(YearMonth.from(today).plusMonths(1)) else thisMonth
            }
            .minByOrNull { it.second }

}

/** "Due on the 5th every month". */
fun RecurringBill.dueDayText(): String {
    val suffix = if (dueDay in 11..13) "th" else when (dueDay % 10) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }
    return "Due on the $dueDay$suffix every month"
}
