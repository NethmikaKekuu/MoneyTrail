package lk.moneytrail.app.domain.repository

import kotlinx.coroutines.flow.Flow
import lk.moneytrail.app.domain.model.MonthClose
import java.time.YearMonth

// The signed-in user's closed months. Failures are returned as an
// lk.moneytrail.app.domain.model.AppException whose message can be shown to the user.
interface MonthCloseRepository {

    /** The close of [month], or null while it hasn't been closed. Updates live. */
    fun observeClose(month: YearMonth): Flow<Result<MonthClose?>>

    /** Saves [close], replacing an earlier close of the same month. */
    suspend fun saveClose(close: MonthClose): Result<Unit>
}
