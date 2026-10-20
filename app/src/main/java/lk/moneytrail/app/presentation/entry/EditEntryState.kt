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
    /** The entry with the user's changes. Its amount comes from [amountDigits] when saving. */
    val draft: Entry? = null,
    val amountDigits: String = "",
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
    val amount: Long
        get() = amountDigits.toLongOrNull() ?: 0L

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
