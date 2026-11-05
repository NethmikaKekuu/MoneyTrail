package lk.moneytrail.app.domain.repository

import kotlinx.coroutines.flow.Flow
import lk.moneytrail.app.domain.model.crypto.CryptoTrade

/**
 * The signed-in user's imported crypto trades.
 * Failures are returned as an AppException whose message can be shown to the user.
 */
interface CryptoRepository {
    /** Trades already saved, for spotting duplicates in a new import. */
    suspend fun getExistingTrades(): List<CryptoTrade>

    /** Every saved trade, again whenever one is added or removed. */
    fun observeTrades(): Flow<Result<List<CryptoTrade>>>

    /** Saves new trades; a trade already saved is overwritten, never doubled. */
    suspend fun saveTrades(trades: List<CryptoTrade>): Result<Unit>

    /** Removes every imported trade, to start again from a fresh export. */
    suspend fun deleteAllTrades(): Result<Unit>
}
