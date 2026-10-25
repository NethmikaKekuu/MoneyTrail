package lk.moneytrail.app.presentation.dashboard

data class DashboardState(
    /** True while the phone has no internet, to show the offline banner above the tabs. */
    val isOffline: Boolean = false,
    /** True when the user is back after a break, to open the catch-up once. */
    val openCatchUp: Boolean = false
)
