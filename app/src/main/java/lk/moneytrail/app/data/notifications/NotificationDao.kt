package lk.moneytrail.app.data.notifications

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Reads and writes the notifications table. */
@Dao
abstract class NotificationDao {

    @Query("SELECT * FROM notifications WHERE uid = :uid AND isRead = 0 ORDER BY createdAt DESC, id DESC")
    abstract fun observeUnread(uid: String): Flow<List<NotificationEntity>>

    /** Read ones are kept for a month (see [deleteReadBefore]), so this is the last month's history. */
    @Query("SELECT * FROM notifications WHERE uid = :uid ORDER BY createdAt DESC, id DESC")
    abstract fun observeAll(uid: String): Flow<List<NotificationEntity>>

    @Query("UPDATE notifications SET isRead = 1 WHERE uid = :uid AND id = :id")
    abstract suspend fun markRead(uid: String, id: Long)

    @Insert
    abstract suspend fun insert(notification: NotificationEntity): Long

    /** Refreshes the row for this kind and day in place, so its id stays the same. Returns the rows changed. */
    @Query("UPDATE notifications SET title = :title, text = :text, createdAt = :createdAt, isRead = 0 WHERE uid = :uid AND kind = :kind AND day = :day")
    abstract suspend fun refresh(uid: String, kind: String, day: String, title: String, text: String, createdAt: Long): Int

    @Query("UPDATE notifications SET isRead = 1 WHERE uid = :uid AND kind = :kind AND isRead = 0 AND day < :day")
    abstract suspend fun markOlderRead(uid: String, kind: String, day: String)

    @Query("UPDATE notifications SET isRead = 1 WHERE uid = :uid AND kind = :kind AND isRead = 0")
    abstract suspend fun markKindRead(uid: String, kind: String)

    @Query("DELETE FROM notifications WHERE uid = :uid AND isRead = 1 AND day < :before")
    abstract suspend fun deleteReadBefore(uid: String, before: String)

    /** Keeps [notification], hands over from earlier ones of its kind and drops old read rows, in one go. */
    @Transaction
    open suspend fun add(notification: NotificationEntity, deleteReadBefore: String) {
        val n = notification
        if (refresh(n.uid, n.kind, n.day, n.title, n.text, n.createdAt) == 0) insert(n)
        markOlderRead(notification.uid, notification.kind, notification.day)
        deleteReadBefore(notification.uid, deleteReadBefore)
    }
}
