package lk.moneytrail.app.domain.repository

import kotlinx.coroutines.flow.Flow
import lk.moneytrail.app.domain.model.AppNotification
import lk.moneytrail.app.domain.model.NotificationKind
import java.time.LocalDate

/**
 * The reminders kept on the phone for the signed-in user, so they can be opened from the
 * bell. Only unread ones are listed; a reminder is read once it has been dealt with.
 */
interface NotificationLogRepository {

    /** The unread reminders, newest first. Updates live; empty when nobody is signed in. */
    fun observeUnread(): Flow<List<AppNotification>>

    /** Every reminder kept on the phone, read or not, newest first: the notification history. Updates live. */
    fun observeAll(): Flow<List<AppNotification>>

    /**
     * Keeps a reminder for [day]. Earlier unread ones of the same [kind] are marked read,
     * since this one replaces them. Adding the same kind and day again shows it as unread again.
     */
    suspend fun add(kind: NotificationKind, title: String, text: String, day: LocalDate)

    /** Marks every unread reminder of [kind] as read: what it asked for is done. */
    suspend fun markKindRead(kind: NotificationKind)

    /** Marks just the reminder [id] as read, when it's opened; the others stay as they are. */
    suspend fun markRead(id: Long)
}
