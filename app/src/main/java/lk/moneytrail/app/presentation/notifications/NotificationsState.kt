package lk.moneytrail.app.presentation.notifications

import lk.moneytrail.app.domain.model.AppNotification

data class NotificationsState(
    /** Every reminder from the last month, newest first, read or not. */
    val items: List<AppNotification> = emptyList(),
    /** True until the list has been read from the phone. */
    val isLoading: Boolean = true
) {
    /** Not opened yet, shown first under "New". */
    val unread: List<AppNotification>
        get() = items.filter { !it.isRead }

    /** Opened or dealt with, kept under "Earlier" as history. */
    val earlier: List<AppNotification>
        get() = items.filter { it.isRead }
}
