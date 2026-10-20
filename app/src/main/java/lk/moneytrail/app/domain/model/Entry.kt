package lk.moneytrail.app.domain.model

import java.time.LocalDate

enum class EntryType { INCOME, EXPENSE }

/** Whether an expense had to be paid (rent, bills) or was a choice (coffee, rides). */
enum class SpendingType { COMMITTED, DISCRETIONARY }

/**
 * CONFIRMED money has really moved. EXPECTED money is planned for its date, e.g. a
 * freelance payment or a bill, and only counts once the user confirms it.
 */
enum class EntryStatus { CONFIRMED, EXPECTED }

/** One income or expense on the calendar: the users/{uid}/entries/{id} document in Firestore. */
data class Entry(
    /** Firestore document id. Empty for an entry that hasn't been saved yet. */
    val id: String,
    val type: EntryType,
    /** In whole rupees (LKR). Always positive; [type] says which way the money went. */
    val amount: Long,
    val category: String,
    val account: String,
    val date: LocalDate,
    val note: String,
    /** Set for expenses only; null for income. */
    val spendingType: SpendingType?,
    val status: EntryStatus = EntryStatus.CONFIRMED
) {
    val isExpected: Boolean
        get() = status == EntryStatus.EXPECTED
}
