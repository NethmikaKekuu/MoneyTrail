package lk.moneytrail.app.presentation.dashboard

sealed interface DashboardState {
    data object Loading : DashboardState

    /** [name] is null when the account has no Firestore profile. */
    data class Ready(val name: String?, val email: String?) : DashboardState

    data class Error(val message: String) : DashboardState
}
