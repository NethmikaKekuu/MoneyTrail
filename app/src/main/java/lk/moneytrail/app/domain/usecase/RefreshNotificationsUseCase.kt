package lk.moneytrail.app.domain.usecase

import lk.moneytrail.app.domain.model.NotificationKind
import lk.moneytrail.app.domain.model.kind
import lk.moneytrail.app.domain.repository.NotificationLogRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Marks a reminder as read once what it asks for is done. It asks [GetRemindersUseCase] what
 * is still pending today, by the same rules as the reminders themselves, and clears every
 * kind that no longer applies. A reminder the user postponed with "Later" is still pending,
 * so it stays unread.
 */
class RefreshNotificationsUseCase @Inject constructor(
    private val getReminders: GetRemindersUseCase,
    private val notificationLog: NotificationLogRepository
) {
    suspend operator fun invoke(today: LocalDate) {
        val pending = getReminders(today).map { it.kind }.toSet()
        NotificationKind.entries
            .filter { it !in pending }
            .forEach { notificationLog.markKindRead(it) }
    }
}
