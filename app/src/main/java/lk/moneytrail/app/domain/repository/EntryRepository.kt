package lk.moneytrail.app.domain.repository

import kotlinx.coroutines.flow.Flow
import lk.moneytrail.app.domain.model.Entry
import java.time.YearMonth

// Works on the signed-in user's entries. Failures are returned as an
// lk.moneytrail.app.domain.model.AppException whose message can be shown to the user.
interface EntryRepository {

    /**
     * The entries dated in [month], oldest first. Emits again whenever an entry is added
     * or changed, including straight after a save while offline. After a failure the flow
     * ends; collect it again to retry.
     */
    fun observeMonth(month: YearMonth): Flow<Result<List<Entry>>>

    /** Saves a new entry. The id of [entry] is ignored; Firestore picks one. */
    suspend fun addEntry(entry: Entry): Result<Unit>
}
