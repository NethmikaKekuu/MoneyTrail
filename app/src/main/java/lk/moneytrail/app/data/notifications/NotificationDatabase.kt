package lk.moneytrail.app.data.notifications

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The notification list on the phone. Unlike MoneyTrailDatabase this isn't a copy of
 * Firestore, so it's kept apart and is never emptied on an upgrade: a change to the
 * tables needs a real migration.
 */
@Database(entities = [NotificationEntity::class], version = 1, exportSchema = false)
abstract class NotificationDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
}
