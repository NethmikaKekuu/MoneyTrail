package lk.moneytrail.app.data.reminders

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import lk.moneytrail.app.domain.repository.ReminderScheduler
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/** Books the reminder check with WorkManager, which runs it even after the app is closed or the phone restarts. */
@Singleton
class WorkManagerReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : ReminderScheduler {

    override fun scheduleDaily() {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(Duration.ofDays(1))
            .setInitialDelay(untilNextCheck(LocalDateTime.now()))
            .build()
        // KEEP: opening the app again doesn't move the time already booked.
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(DAILY_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    override fun checkNow() {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(workDataOf(ReminderWorker.KEY_FORCE to true))
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    companion object {
        private const val DAILY_WORK = "daily-reminders"
        private val CHECK_TIME: LocalTime = LocalTime.of(9, 0)

        /** Time from [now] until the next 9:00 in the morning. */
        fun untilNextCheck(now: LocalDateTime): Duration {
            var next = now.toLocalDate().atTime(CHECK_TIME)
            if (!next.isAfter(now)) next = next.plusDays(1)
            return Duration.between(now, next)
        }
    }
}
