package lk.moneytrail.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The database on the phone. It only holds a copy of Firestore, so raising the version
 * after a schema change just empties it and the Firestore listeners fill it again.
 */
@Database(entities = [EntryEntity::class], version = 1, exportSchema = false)
abstract class MoneyTrailDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
}
