package lk.moneytrail.app.domain.model.crypto

import java.time.LocalDateTime

enum class TradeSide {
    BUY,
    SELL
}

/**
 * Represents a single parsed crypto trade entry from Binance.
 */
data class CryptoTrade(
    val tradeId: String = "",
    val date: LocalDateTime,
    val pair: String,
    val type: TradeSide,
    val price: Double,
    val amount: Double,
    val total: Double,
    val fee: Double = 0.0,
    val feeCoin: String = "",
    val costBasis: Double? = null,
)

/**
 * A row from the CSV file that could not be parsed into a valid trade.
 *
 * @property rowIndex 1-based index of the CSV row (header counts as row 1).
 * @property rawData Map of column header to string value for the rejected row.
 * @property reason Human-readable explanation of why validation failed.
 */
data class RejectedTrade(
    val rowIndex: Int,
    val rawData: Map<String, String>,
    val reason: String,
)

/**
 * Result of parsing a Binance CSV file containing trade records.
 *
 * @property read Valid trades successfully parsed and not found in existing records.
 * @property rejected CSV rows that failed validation along with the failure reason.
 * @property duplicates Valid trades that match existing database records.
 */
data class CsvParseResult(
    val read: List<CryptoTrade>,
    val rejected: List<RejectedTrade>,
    val duplicates: List<CryptoTrade>,
)

/**
 * Exception thrown when the CSV file headers do not match expected Binance columns.
 *
 * @property missingColumns List of mandatory column names missing from the header.
 */
class InvalidCsvHeaderException(
    val missingColumns: List<String>,
) : Exception("Missing required Binance CSV columns: ${missingColumns.joinToString(", ")}")
