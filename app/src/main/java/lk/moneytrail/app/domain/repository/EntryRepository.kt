package lk.moneytrail.app.domain.repository

import kotlinx.coroutines.flow.Flow
import lk.moneytrail.app.domain.model.Entry
import java.time.LocalDate
import java.time.YearMonth

// Works on the signed-in user's entries. Failures are returned as an
// lk.moneytrail.app.domain.model.AppException whose message can be shown to the user.
interface EntryRepository {

    /**
     * The entries dated from [from] to [to], both included, oldest first. Emits again
     * whenever an entry is added, changed or deleted, including straight after a save
     * while offline. After a failure it stops updating; collect it again to retry.
     */
    fun observeBetween(from: LocalDate, to: LocalDate): Flow<Result<List<Entry>>>

    /** Every expected entry, whatever its date, oldest first. Updates live. */
    fun observeExpected(): Flow<Result<List<Entry>>>

    /** The entries dated in [month]; see [observeBetween]. */
    fun observeMonth(month: YearMonth): Flow<Result<List<Entry>>> =
        observeBetween(month.atDay(1), month.atEndOfMonth())

    /** The entry with this id, or null if it has been deleted. */
    suspend fun getEntry(id: String): Result<Entry?>

    /** Saves a new entry. The id of [entry] is ignored; Firestore picks one. */
    suspend fun addEntry(entry: Entry): Result<Unit>

    /** Saves changes to an existing entry, found by its id. Its creation time is kept. */
    suspend fun updateEntry(entry: Entry): Result<Unit>

    suspend fun deleteEntry(id: String): Result<Unit>

    /** The day the user last saved an entry (not the entry's own date); null when they never have. */
    suspend fun lastLoggedDate(): Result<LocalDate?>
}
