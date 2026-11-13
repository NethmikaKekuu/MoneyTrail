package lk.moneytrail.app.data.notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import lk.moneytrail.app.data.reminders.systemNotificationId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
    @ApplicationContext private val context: Context,
    private val dao: NotificationDao,
    private val authRepository: AuthRepository
) : NotificationLogRepository {

    override fun observeUnread(): Flow<List<AppNotification>> {
        // Emits an empty list when nobody is signed in, so the screen doesn't wait forever.
        val uid = authRepository.currentUserId ?: return flowOf(emptyList())
        return dao.observeUnread(uid).map { rows -> rows.mapNotNull { it.toNotification() } }
    }

    override fun observeAll(): Flow<List<AppNotification>> {
        val uid = authRepository.currentUserId ?: return flowOf(emptyList())
        return dao.observeAll(uid).map { rows -> rows.mapNotNull { it.toNotification() } }
    }

    override suspend fun markRead(id: Long) {
        val uid = authRepository.currentUserId ?: return
        dao.markRead(uid, id)
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
        // Done in the app, so take it out of the phone's notification shade too.
        NotificationManagerCompat.from(context).cancel(kind.systemNotificationId)
    }

    private companion object {
        /** Read reminders are only kept for a month. */
        const val KEEP_READ_DAYS = 30L
    }
}
