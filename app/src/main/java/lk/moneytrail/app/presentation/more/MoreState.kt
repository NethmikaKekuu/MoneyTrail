package lk.moneytrail.app.presentation.more


sealed interface MoreState {
    data object Loading : MoreState

    /** [name] is null when the account has no Firestore profile. */
    data class Ready(
        val name: String?,
        val email: String?,
        /** "Due 25 Oct", "Due today", "Overdue" or "Confirmed" for this month's salary; null without one. */
        val salaryBadge: String? = null
    ) : MoreState

    data class Error(val message: String) : MoreState
}
