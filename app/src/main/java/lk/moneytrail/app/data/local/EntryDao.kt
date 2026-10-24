package lk.moneytrail.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Reads and writes the entries table. The Flows emit again whenever the table changes. */
@Dao
abstract class EntryDao {

    @Query("SELECT * FROM entries WHERE uid = :uid AND date BETWEEN :from AND :to ORDER BY date, createdAt")
    abstract fun observeBetween(uid: String, from: String, to: String): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE uid = :uid AND status = 'EXPECTED' ORDER BY date, createdAt")
    abstract fun observeExpected(uid: String): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE uid = :uid AND id = :id")
    abstract suspend fun getEntry(uid: String, id: String): EntryEntity?

    @Upsert
    abstract suspend fun upsert(entries: List<EntryEntity>)

    @Query("DELETE FROM entries WHERE uid = :uid AND date BETWEEN :from AND :to")
    abstract suspend fun deleteBetween(uid: String, from: String, to: String)

    @Query("DELETE FROM entries WHERE uid = :uid AND status = 'EXPECTED'")
    abstract suspend fun deleteExpected(uid: String)

    /**
     * Makes the rows dated [from] to [to] match what Firestore sent, so entries deleted
     * elsewhere go too. One transaction, so screens never see the dates empty in between.
     */
    @Transaction
    open suspend fun replaceBetween(uid: String, from: String, to: String, entries: List<EntryEntity>) {
        deleteBetween(uid, from, to)
        upsert(entries)
    }

    /** The same as [replaceBetween], for every expected entry. */
    @Transaction
    open suspend fun replaceExpected(uid: String, entries: List<EntryEntity>) {
        deleteExpected(uid)
        upsert(entries)
    }
}
