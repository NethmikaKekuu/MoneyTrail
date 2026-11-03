package lk.moneytrail.app.presentation.notifications

import lk.moneytrail.app.domain.model.AppNotification

data class NotificationsState(
    /** The unread reminders, newest first. */
    val items: List<AppNotification> = emptyList(),
    /** True until the list has been read from the phone. */
    val isLoading: Boolean = true,
    /** The reminder whose action sheet is open. */
    val selectedId: Long? = null
) {
    /** Null once the reminder has been dealt with, which also closes its sheet. */
    val selected: AppNotification?
        get() = items.find { it.id == selectedId }
}
