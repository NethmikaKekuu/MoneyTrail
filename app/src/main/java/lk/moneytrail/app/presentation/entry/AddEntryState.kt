package lk.moneytrail.app.presentation.entry

import lk.moneytrail.app.domain.model.Accounts
import lk.moneytrail.app.domain.model.Categories
import lk.moneytrail.app.domain.model.CustomCategory
import lk.moneytrail.app.domain.model.EntryType
import java.time.LocalDate

data class AddEntryState(
    val type: EntryType = EntryType.EXPENSE,
    /** The digits typed on the keypad, e.g. "2400". Empty until something is typed. */
    val amountDigits: String = "",
    val category: String = Categories.namesFor(EntryType.EXPENSE).first(),
    val account: String = Accounts.defaults.first(),
    /** Accounts to pick from: the user's own after onboarding, otherwise the defaults. */
    val accounts: List<String> = Accounts.defaults,
    /** Categories the user added, shown after the built-in ones. */
    val customCategories: List<CustomCategory> = emptyList(),
    /** True while the "New category" dialog is open. */
    val isAddingCategory: Boolean = false,
    val isSavingCategory: Boolean = false,
    val categoryError: String? = null,
    val date: LocalDate,
    val note: String = "",
    /** Money planned for [date] rather than money that has already moved. */
    val isExpected: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    /** Set once the entry is saved; the screen then closes. */
    val isSaved: Boolean = false
) {
    val amount: Long
        get() = amountDigits.toLongOrNull() ?: 0L

    /** AdSense is paid in dollars, so it gets its own payment screen. */
    val canEnterUsd: Boolean
        get() = type == EntryType.INCOME && category == Categories.ADSENSE

    val categories: List<String>
        get() = Categories.namesFor(type, customCategories)
}
