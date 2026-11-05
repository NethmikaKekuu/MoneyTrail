package lk.moneytrail.app.domain.model.crypto

import java.time.LocalDateTime

enum class TradeSide {
    BUY,
    SELL,
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

/**
 * Represents the realised profit and loss for a matched sell trade fill.
 *
 * @property sellTradeId ID of the sell trade.
 * @property pair The crypto trading pair (e.g., "BTCUSDT").
 * @property sellDate Date and time when the sell trade occurred.
 * @property amount Quantity of crypto asset sold in this matched fill.
 * @property sellPrice Selling price per unit.
 * @property proceeds Total revenue from sale (amount * sellPrice).
 * @property costBasis Cost basis of the matched buy lot(s).
 * @property fee Total fee allocated to this sell trade match (proportional buy fee + sell fee).
 * @property realisedPnl Net profit or loss (proceeds - costBasis - fee).
 */
data class CryptoRealisedPnl(
    val sellTradeId: String = "",
    val pair: String,
    val sellDate: LocalDateTime,
    val amount: Double,
    val sellPrice: Double,
    val proceeds: Double,
    val costBasis: Double,
    val fee: Double,
    val realisedPnl: Double,
)
