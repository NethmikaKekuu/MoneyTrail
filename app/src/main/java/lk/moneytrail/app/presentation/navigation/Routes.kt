package lk.moneytrail.app.presentation.navigation

import android.net.Uri
import java.time.LocalDate

object Routes {
    const val SPLASH = "splash"
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
    const val GOAL = "goal"
    const val SALARY = "salary"
    const val CATCH_UP = "catch_up"

    /** Optional yyyy-MM month for the monthly close and the statement; last month when left out. */
    const val MONTH_ARG = "month"
    const val MONTHLY_CLOSE = "monthly_close?$MONTH_ARG={$MONTH_ARG}"
    const val STATEMENT = "statement?$MONTH_ARG={$MONTH_ARG}"
    fun monthlyClose(month: java.time.YearMonth): String = "monthly_close?$MONTH_ARG=$month"
    fun statement(month: java.time.YearMonth?): String = if (month == null) "statement" else "statement?$MONTH_ARG=$month"
    const val FREELANCE = "freelance"
    const val ADD_PROJECT = "add_project"
    const val RECORD_PAYMENT_PROJECT_ARG = "projectId"
    /** Optional id of the project whose next unpaid milestone the payment screen starts on. */
    const val RECORD_FREELANCE_PAYMENT = "record_freelance_payment?$RECORD_PAYMENT_PROJECT_ARG={$RECORD_PAYMENT_PROJECT_ARG}"
    fun recordFreelancePayment(projectId: String?): String =
        if (projectId == null) "record_freelance_payment" else "record_freelance_payment?$RECORD_PAYMENT_PROJECT_ARG=$projectId"

    const val NOTIFICATIONS = "notifications"

    /** Set on the dashboard by the notification list: which reminder's action the user chose, a NotificationKind name. */
    const val RESULT_NOTIFICATION_ACTION = "notificationAction"

    /** Optional yyyy-MM-dd day for a new entry to start on, e.g. the day picked on the calendar. */
    const val ADD_ENTRY_DATE_ARG = "date"
    const val ADD_ENTRY = "add_entry?$ADD_ENTRY_DATE_ARG={$ADD_ENTRY_DATE_ARG}"

    fun addEntry(date: LocalDate): String = "add_entry?$ADD_ENTRY_DATE_ARG=$date"

    /** Firestore id of the entry to edit. */
    const val EDIT_ENTRY_ID_ARG = "entryId"
    const val EDIT_ENTRY = "edit_entry/{$EDIT_ENTRY_ID_ARG}"

    /** Optional yyyy-MM-dd day the AdSense payment screen starts on, passed from the new entry. */
    const val USD_PAYOUT_DATE_ARG = "date"
    const val USD_PAYOUT = "usd_payout?$USD_PAYOUT_DATE_ARG={$USD_PAYOUT_DATE_ARG}"

    fun usdPayout(date: LocalDate): String = "usd_payout?$USD_PAYOUT_DATE_ARG=$date"

    fun editEntry(id: String): String = "edit_entry/${Uri.encode(id)}"
}
