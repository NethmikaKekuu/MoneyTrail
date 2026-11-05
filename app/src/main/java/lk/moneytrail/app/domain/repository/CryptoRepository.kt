package lk.moneytrail.app.domain.repository

import lk.moneytrail.app.domain.model.crypto.CryptoTrade

/**
 * Repository interface for managing crypto trades in MoneyTrail.
 */
interface CryptoRepository {
    /**
     * Retrieves existing crypto trades from the database for duplicate matching.
     */
    suspend fun getExistingTrades(): List<CryptoTrade>
}
