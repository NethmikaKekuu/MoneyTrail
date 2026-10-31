package lk.moneytrail.app.data.notifications

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Reads and writes the notifications table. */
@Dao
abstract class NotificationDao {

    @Query("SELECT * FROM notifications WHERE uid = :uid AND isRead = 0 ORDER BY createdAt DESC, id DESC")
    abstract fun observeUnread(uid: String): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(notification: NotificationEntity): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE uid = :uid AND kind = :kind AND isRead = 0 AND day < :day")
    abstract suspend fun markOlderRead(uid: String, kind: String, day: String)

    @Query("UPDATE notifications SET isRead = 1 WHERE uid = :uid AND kind = :kind AND isRead = 0")
    abstract suspend fun markKindRead(uid: String, kind: String)

    @Query("DELETE FROM notifications WHERE uid = :uid AND isRead = 1 AND day < :before")
    abstract suspend fun deleteReadBefore(uid: String, before: String)

    /** Keeps [notification], hands over from earlier ones of its kind and drops old read rows, in one go. */
    @Transaction
    open suspend fun add(notification: NotificationEntity, deleteReadBefore: String) {
        insert(notification)
        markOlderRead(notification.uid, notification.kind, notification.day)
        deleteReadBefore(notification.uid, deleteReadBefore)
    }
}
