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
    val status: EntryStatus = EntryStatus.CONFIRMED,
    /**
     * For a salary: the part of [amount] that was a bonus on top of the base pay, in LKR.
     * [amount] is always the whole sum, so totals need nothing extra. 0 for other entries.
     */
    val bonus: Long = 0,
    /**
     * For income paid in US dollars (AdSense): the dollars earned, in cents. [amount] is
     * then the rupees that actually arrived, so every total keeps working in LKR. 0 otherwise.
     */
    val usdCents: Long = 0,
    /** The rate that payout really got, in hundredths of a rupee per dollar ([amount] ÷ dollars). 0 when [usdCents] is 0. */
    val effectiveRate: Long = 0,
    /** Rupees lost to bank and conversion charges against that day's market rate. 0 when unknown. */
    val feeLkr: Long = 0
) {
    val isUsd: Boolean
        get() = usdCents > 0

    val isExpected: Boolean
        get() = status == EntryStatus.EXPECTED

    /** Only a salary can carry a bonus. */
    val isSalary: Boolean
        get() = type == EntryType.INCOME && category == Categories.SALARY

    /** A salary's base pay without the bonus; the whole amount for other entries. */
    val baseAmount: Long
        get() = amount - bonus
}
