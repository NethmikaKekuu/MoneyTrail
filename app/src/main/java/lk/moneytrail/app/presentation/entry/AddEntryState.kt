package lk.moneytrail.app.presentation.entry

import lk.moneytrail.app.domain.model.Accounts
import lk.moneytrail.app.domain.model.Categories
import lk.moneytrail.app.domain.model.EntryType
import java.time.LocalDate

data class AddEntryState(
    val type: EntryType = EntryType.EXPENSE,
    /** The digits typed on the keypad, e.g. "2400". Empty until something is typed. */
    val amountDigits: String = "",
    val category: String = Categories.namesFor(EntryType.EXPENSE).first(),
    val account: String = Accounts.defaults.first(),
    val date: LocalDate,
    val note: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    /** Set once the entry is saved; the screen then closes. */
    val isSaved: Boolean = false
) {
    val amount: Long
        get() = amountDigits.toLongOrNull() ?: 0L

    val categories: List<String>
        get() = Categories.namesFor(type)

    val accounts: List<String>
        get() = Accounts.defaults
}
