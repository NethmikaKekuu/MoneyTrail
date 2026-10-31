package lk.moneytrail.app.data.notifications

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import lk.moneytrail.app.domain.model.AppNotification
import lk.moneytrail.app.domain.model.NotificationKind
import lk.moneytrail.app.domain.repository.AuthRepository
import lk.moneytrail.app.domain.repository.NotificationLogRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomNotificationLogRepository @Inject constructor(
    private val dao: NotificationDao,
    private val authRepository: AuthRepository
) : NotificationLogRepository {

    override fun observeUnread(): Flow<List<AppNotification>> {
        val uid = authRepository.currentUserId ?: return emptyFlow()
        return dao.observeUnread(uid).map { rows -> rows.mapNotNull { it.toNotification() } }
    }

    override suspend fun add(kind: NotificationKind, title: String, text: String, day: LocalDate) {
        val uid = authRepository.currentUserId ?: return
        val row = NotificationEntity(
            uid = uid,
            kind = kind.name,
            title = title,
            text = text,
            day = day.toString(),
            createdAt = System.currentTimeMillis(),
            isRead = false
        )
        dao.add(row, deleteReadBefore = day.minusDays(KEEP_READ_DAYS).toString())
    }

    override suspend fun markKindRead(kind: NotificationKind) {
        val uid = authRepository.currentUserId ?: return
        dao.markKindRead(uid, kind.name)
    }

    private companion object {
        /** Read reminders are only kept for a month. */
        const val KEEP_READ_DAYS = 30L
    }
}
