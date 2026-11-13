package lk.moneytrail.app.data.reminders

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.withTimeoutOrNull
import lk.moneytrail.app.di.ReminderEntryPoint
import java.time.LocalDate

/**
 * The background check WorkManager runs every morning, with or without the app open:
 * works out the reminders and shows them as system notifications.
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val dependencies = EntryPointAccessors.fromApplication(applicationContext, ReminderEntryPoint::class.java)
        // Nobody signed in, nobody to remind.
        if (dependencies.authRepository().currentUserId == null) return Result.success()

        val today = LocalDate.now()
        // Offline, Firestore answers from the phone; the limit only stops a stuck check.
        val reminders = withTimeoutOrNull(CHECK_TIMEOUT_MS) { dependencies.getReminders()(today) }
        if (reminders == null) {
            Log.w(TAG, "The reminder check took too long; trying again at the next run")
            return Result.success()
        }
        ReminderNotifier(applicationContext, dependencies.notificationLog()).show(reminders, today, force = inputData.getBoolean(KEY_FORCE, false))
        return Result.success()
    }

    companion object {
        /** Shows reminders even if they were already shown today ("Check reminders now"). */
        const val KEY_FORCE = "force"
        private const val TAG = "ReminderWorker"
        private const val CHECK_TIMEOUT_MS = 60_000L
    }
}
