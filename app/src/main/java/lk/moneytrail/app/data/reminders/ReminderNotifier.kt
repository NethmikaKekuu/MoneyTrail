package lk.moneytrail.app.data.reminders

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import lk.moneytrail.app.MainActivity
import lk.moneytrail.app.R
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.Reminder
import lk.moneytrail.app.domain.model.kind
import lk.moneytrail.app.domain.repository.NotificationLogRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Shows reminders as system notifications in one "Reminders" channel. Each kind has its
 * own notification and shows at most once a day, unless forced; each is also kept in the
 * app's notification list; tapping one opens the app,
 * where the matching card is waiting.
 */
internal class ReminderNotifier(
    private val context: Context,
    private val notificationLog: NotificationLogRepository
) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val manager = NotificationManagerCompat.from(context)

    // Checked just before: notifications are on, which on Android 13+ includes the permission.
    @SuppressLint("MissingPermission")
    suspend fun show(reminders: List<Reminder>, today: LocalDate, force: Boolean) {
        // Kept in the app's notification list even when the phone doesn't show them.
        reminders.forEach { reminder ->
            val (title, text) = textFor(reminder, today)
            notificationLog.add(reminder.kind, title, text, today)
        }
        if (!manager.areNotificationsEnabled()) return
        createChannel()

        reminders.forEach { reminder ->
            val key = keyFor(reminder)
            if (!force && prefs.getString(key, null) == today.toString()) return@forEach

            val (title, text) = textFor(reminder, today)
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(BRAND_ORANGE)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(openAppIntent())
                .setAutoCancel(true)
                .build()
            try {
                manager.notify(idFor(reminder), notification)
                prefs.edit().putString(key, today.toString()).apply()
            } catch (e: SecurityException) {
                Log.w(TAG, "Notifications were turned off while checking", e)
            }
        }
    }

    private fun createChannel() {
        // Creating it again does nothing, so it's simply done before each check.
        val channel = NotificationChannel(CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Money due, paying yourself first, and catching up after a break"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun textFor(reminder: Reminder, today: LocalDate): Pair<String, String> = when (reminder) {
        is Reminder.MoneyDue -> moneyDueText(reminder, today)
        is Reminder.PayYourselfFirst ->
            "Pay yourself first" to "Move LKR ${amount(reminder.amount)} to Savings now, before it gets spent."
        is Reminder.CatchUp ->
            "It's been ${reminder.daysAway} days" to
                "Catch up in about 2 minutes: confirm what's due and balance your accounts."
    }

    private fun moneyDueText(reminder: Reminder.MoneyDue, today: LocalDate): Pair<String, String> {
        val entries = reminder.entries
        if (entries.size > 1) {
            val names = entries.take(3).joinToString(", ") { it.category } + if (entries.size > 3) "…" else ""
            return "${entries.size} things to check" to "$names. Tap to confirm what happened."
        }
        val entry = entries.first()
        val isIncome = entry.type == EntryType.INCOME
        val whenText = if (entry.date == today) "today" else "on ${entry.date.format(DayFormatter)}"
        val title = if (isIncome) "${entry.category} expected $whenText" else "${entry.category} due $whenText"
        val question = if (isIncome) "Did it come in?" else "Did it go out?"
        return title to "LKR ${amount(entry.amount)}. $question Tap to check."
    }

    private fun amount(value: Long): String = String.format(Locale.US, "%,d", value)

    private fun keyFor(reminder: Reminder): String = "shown_" + reminder::class.java.simpleName

    private fun idFor(reminder: Reminder): Int = when (reminder) {
        is Reminder.MoneyDue -> 1
        is Reminder.PayYourselfFirst -> 2
        is Reminder.CatchUp -> 3
    }

    private companion object {
        const val TAG = "ReminderNotifier"
        const val CHANNEL_ID = "reminders"
        const val PREFS_NAME = "moneytrail_reminders"
        /** Trail orange from the logo, for the icon's tint. */
        const val BRAND_ORANGE = 0xFFF97316.toInt()
        val DayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    }
}
