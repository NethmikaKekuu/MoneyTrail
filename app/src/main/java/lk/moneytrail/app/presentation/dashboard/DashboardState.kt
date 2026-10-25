package lk.moneytrail.app.presentation.dashboard

data class DashboardState(
    /** True while the phone has no internet, to show the offline banner above the tabs. */
    val isOffline: Boolean = false
)
