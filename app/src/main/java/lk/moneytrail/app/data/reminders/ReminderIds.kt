package lk.moneytrail.app.data.reminders

import lk.moneytrail.app.domain.model.NotificationKind

/** The system notification id for each kind, so a reminder can be shown and later taken down again. */
internal val NotificationKind.systemNotificationId: Int
    get() = when (this) {
        NotificationKind.MONEY_DUE -> 1
        NotificationKind.PAY_YOURSELF_FIRST -> 2
        NotificationKind.CATCH_UP -> 3
    }
