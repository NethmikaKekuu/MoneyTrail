package lk.moneytrail.app.domain.repository

import kotlinx.coroutines.flow.Flow
import lk.moneytrail.app.domain.model.RecurringBill
import java.time.YearMonth

/**
 * Works on the signed-in user's recurring bills. Failures are returned as an
 * lk.moneytrail.app.domain.model.AppException whose message can be shown to the user.
 */
interface RecurringBillRepository {

    /** Every bill, stopped ones included, sorted by due day then name. Updates live. */
    fun observeBills(): Flow<Result<List<RecurringBill>>>

    /** Saves [bill]: a new one when its id is empty, otherwise changes the existing one. */
    suspend fun saveBill(bill: RecurringBill): Result<Unit>

    suspend fun deleteBill(id: String): Result<Unit>

    /** Records that the bill is on the calendar up to [month], or clears it when [month] is null. */
    suspend fun markCreated(id: String, month: YearMonth?): Result<Unit>
}
