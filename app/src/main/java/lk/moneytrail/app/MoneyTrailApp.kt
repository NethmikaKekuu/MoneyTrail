package lk.moneytrail.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import lk.moneytrail.app.domain.repository.ReminderScheduler
import javax.inject.Inject

@HiltAndroidApp
class MoneyTrailApp : Application() {

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    override fun onCreate() {
        super.onCreate()
        // Books the 9:00 reminder check once; after that WorkManager keeps it going,
        // even when the app isn't opened.
        reminderScheduler.scheduleDaily()
    }
}
