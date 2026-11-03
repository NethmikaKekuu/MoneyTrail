package lk.moneytrail.app.domain.model

import java.time.LocalDate

/** What a reminder is about; one row per kind and day in the notification list. */
enum class NotificationKind { MONEY_DUE, PAY_YOURSELF_FIRST, CATCH_UP, INVOICE_OVERDUE }

val Reminder.kind: NotificationKind
    get() = when (this) {
        is Reminder.MoneyDue -> NotificationKind.MONEY_DUE
        is Reminder.PayYourselfFirst -> NotificationKind.PAY_YOURSELF_FIRST
        is Reminder.CatchUp -> NotificationKind.CATCH_UP
        is Reminder.InvoiceOverdue -> NotificationKind.INVOICE_OVERDUE
    }

/**
 * A reminder kept in the app's notification list. It stays there, unread, until what it
 * asks for has been done.
 */
data class AppNotification(
    val id: Long,
    val kind: NotificationKind,
    val title: String,
    val text: String,
    val day: LocalDate,
    /** Milliseconds; newest first in the list. */
    val createdAt: Long,
    /** Read once opened or once what it asked for is done. Read ones stay in the list as history. */
    val isRead: Boolean = false
)
