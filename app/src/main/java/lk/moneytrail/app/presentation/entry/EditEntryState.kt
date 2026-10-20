package lk.moneytrail.app.presentation.entry

import lk.moneytrail.app.domain.model.Accounts
import lk.moneytrail.app.domain.model.Categories
import lk.moneytrail.app.domain.model.CustomCategory
import lk.moneytrail.app.domain.model.Entry

data class EditEntryState(
    /** True until the entry has loaded. */
    val isLoading: Boolean = true,
    /** Why the entry couldn't be shown, e.g. it was deleted on another phone. */
    val loadError: String? = null,
    /** The entry as saved, to show what changed ("was Shopping"). */
    val original: Entry? = null,
    /** The entry with the user's changes. Its amount comes from [amountDigits] and [bonusDigits] when saving. */
    val draft: Entry? = null,
    /** The amount, or for a salary the base pay without the bonus. */
    val amountDigits: String = "",
    /** A salary's bonus, added to its base pay. Empty for none. */
    val bonusDigits: String = "",
    /** The user's accounts from onboarding, or the defaults. */
    val accounts: List<String> = Accounts.defaults,
    /** Categories the user added in Settings, on a new entry or during onboarding. */
    val customCategories: List<CustomCategory> = emptyList(),
    val isSaving: Boolean = false,
    /** True while the "Delete this entry?" dialog is open. */
    val isConfirmingDelete: Boolean = false,
    val errorMessage: String? = null,
    /** Set once the entry is saved or deleted; the screen then closes. */
    val isDone: Boolean = false
) {
    val isSalary: Boolean
        get() = draft?.isSalary == true

    val bonus: Long
        get() = if (isSalary) bonusDigits.toLongOrNull() ?: 0L else 0L

    /** The whole amount to save: for a salary, the base pay plus the bonus. */
    val amount: Long
        get() = (amountDigits.toLongOrNull() ?: 0L) + bonus

    /** Categories to pick from, keeping the entry's own even if the user has since deleted it. */
    val categories: List<String>
        get() {
            val type = draft?.type ?: return emptyList()
            val own = original?.takeIf { it.type == type }?.category
            return (Categories.namesFor(type, customCategories) + listOfNotNull(own)).distinct()
        }

    /** Accounts to pick from, keeping the entry's own even if it's no longer in the list. */
    val accountChoices: List<String>
        get() = (accounts + listOfNotNull(original?.account)).distinct()
}
