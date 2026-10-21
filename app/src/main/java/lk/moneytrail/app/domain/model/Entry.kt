package lk.moneytrail.app.domain.model

import java.time.LocalDate

enum class EntryType { INCOME, EXPENSE, TRANSFER }

/** Whether an expense had to be paid (rent, bills) or was a choice (coffee, rides). */
enum class SpendingType { COMMITTED, DISCRETIONARY }

/**
 * CONFIRMED money has really moved. EXPECTED money is planned for its date, e.g. a
 * freelance payment or a bill, and only counts once the user confirms it.
 */
enum class EntryStatus { CONFIRMED, EXPECTED }

/** One income, expense or transfer on the calendar: the users/{uid}/entries/{id} document in Firestore. */
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
    /** Set for expenses only; null for income and transfers. */
    val spendingType: SpendingType?,
    val status: EntryStatus = EntryStatus.CONFIRMED,
    /**
     * For a salary: the part of [amount] that was a bonus on top of the base pay, in LKR.
     * [amount] is always the whole sum, so totals need nothing extra. 0 for other entries.
     */
    val bonus: Long = 0,
    /** For transfers: source account id/name. */
    val fromAccountId: String = "",
    /** For transfers: destination account id/name. */
    val toAccountId: String = ""
) {
    val isExpected: Boolean
        get() = status == EntryStatus.EXPECTED

    /** Only a salary can carry a bonus. */
    val isSalary: Boolean
        get() = type == EntryType.INCOME && category == Categories.SALARY

    val isTransfer: Boolean
        get() = type == EntryType.TRANSFER

    /** A salary's base pay without the bonus; the whole amount for other entries. */
    val baseAmount: Long
        get() = amount - bonus
}
