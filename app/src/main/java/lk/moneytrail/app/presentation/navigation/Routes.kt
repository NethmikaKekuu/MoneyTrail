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
    const val FREELANCE = "freelance"
    const val ADD_PROJECT = "add_project"
    const val RECORD_FREELANCE_PAYMENT = "record_freelance_payment"

    /** Optional yyyy-MM-dd day for a new entry to start on, e.g. the day picked on the calendar. */
    const val ADD_ENTRY_DATE_ARG = "date"
    const val ADD_ENTRY = "add_entry?$ADD_ENTRY_DATE_ARG={$ADD_ENTRY_DATE_ARG}"

    fun addEntry(date: LocalDate): String = "add_entry?$ADD_ENTRY_DATE_ARG=$date"

    /** Firestore id of the entry to edit. */
    const val EDIT_ENTRY_ID_ARG = "entryId"
    const val EDIT_ENTRY = "edit_entry/{$EDIT_ENTRY_ID_ARG}"

    fun editEntry(id: String): String = "edit_entry/${Uri.encode(id)}"
}
