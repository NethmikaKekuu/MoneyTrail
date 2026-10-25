package lk.moneytrail.app.domain.repository

/** Runs the reminder check in the background, so notifications come with the app closed. */
interface ReminderScheduler {

    /** Checks every morning at 9:00. Calling it again keeps the schedule already set. */
    fun scheduleDaily()

    /** Checks once now, showing reminders even if they were already shown today. */
    fun checkNow()
}
