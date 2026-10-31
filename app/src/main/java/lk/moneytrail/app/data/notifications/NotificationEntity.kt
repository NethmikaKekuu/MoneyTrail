package lk.moneytrail.app.data.notifications

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import lk.moneytrail.app.domain.model.AppNotification
import lk.moneytrail.app.domain.model.NotificationKind
import java.time.LocalDate

/** One reminder in the notification list. A kind shows at most once a day, so adding it again replaces the row. */
@Entity(tableName = "notifications", indices = [Index("uid", "kind", "day", unique = true)])
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: String,
    val kind: String,
    val title: String,
    val text: String,
    /** yyyy-MM-dd of the day the reminder was for. */
    val day: String,
    val createdAt: Long,
    val isRead: Boolean
)

/** Null for a row with an unknown kind or an unreadable day, which is skipped. */
fun NotificationEntity.toNotification(): AppNotification? {
    val notificationKind = NotificationKind.entries.find { it.name == kind } ?: return null
    val date = runCatching { LocalDate.parse(day) }.getOrNull() ?: return null
    return AppNotification(id, notificationKind, title, text, date, createdAt)
}
