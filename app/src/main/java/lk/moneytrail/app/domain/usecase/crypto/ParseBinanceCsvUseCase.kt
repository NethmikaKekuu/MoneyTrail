package lk.moneytrail.app.domain.usecase.crypto

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lk.moneytrail.app.domain.model.AppException
import lk.moneytrail.app.domain.model.crypto.CryptoTrade
import lk.moneytrail.app.domain.model.crypto.CsvParseResult
import lk.moneytrail.app.domain.model.crypto.InvalidCsvHeaderException
import lk.moneytrail.app.domain.model.crypto.RejectedTrade
import lk.moneytrail.app.domain.model.crypto.TradeSide
import lk.moneytrail.app.domain.repository.CryptoRepository
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import javax.inject.Inject

/**
 * Use case that reads a Binance CSV export file via Android's Storage Access Framework ([Uri]),
 * validates mandatory headers, parses trade rows, and identifies duplicate records against
 * the existing local database records.
 */
class ParseBinanceCsvUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val cryptoRepository: CryptoRepository,
) {

    suspend operator fun invoke(uri: Uri): Result<CsvParseResult> = withContext(Dispatchers.IO) {
        val inputStream = try {
            context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(AppException("Could not open file at provided URI."))
        } catch (e: Exception) {
            return@withContext Result.failure(AppException("Failed to access CSV file: ${e.message}", e))
        }

        inputStream.use { stream ->
            parseInputStream(stream)
        }
    }

    /**
     * Parses the CSV input stream, validating headers and checking parsed trades for duplicates.
     */
    suspend fun parseInputStream(inputStream: InputStream): Result<CsvParseResult> = withContext(Dispatchers.IO) {
        try {
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            val lines = reader.lineSequence().filter { it.isNotBlank() }.toList()

            if (lines.isEmpty()) {
                return@withContext Result.failure(
                    InvalidCsvHeaderException(REQUIRED_HEADER_GROUPS.keys.toList()),
                )
            }

            val headerTokens = parseCsvLine(lines.first())
            val headerIndexMap = headerTokens.mapIndexed { index, name ->
                cleanHeaderName(name) to index
            }.toMap()

            // Validate mandatory column presence
            val missingColumns = mutableListOf<String>()
            val columnMap = mutableMapOf<String, Int>()

            for ((logicalName, aliases) in REQUIRED_HEADER_GROUPS) {
                val foundIndex = aliases.firstNotNullOfOrNull { alias -> headerIndexMap[alias] }
                if (foundIndex != null) {
                    columnMap[logicalName] = foundIndex
                } else {
                    missingColumns.add(logicalName)
                }
            }

            if (missingColumns.isNotEmpty()) {
                return@withContext Result.failure(InvalidCsvHeaderException(missingColumns))
            }

            // Map optional columns if present
            val tradeIdIndex = findIndex(headerIndexMap, "trade id", "tradeid", "id", "order id", "orderid")
            val totalIndex = findIndex(headerIndexMap, "total", "total amount", "volume")
            val feeIndex = findIndex(headerIndexMap, "fee", "transaction fee")
            val feeCoinIndex = findIndex(headerIndexMap, "fee coin", "fee symbol", "fee asset")
            val costBasisIndex = findIndex(headerIndexMap, "cost basis", "costbasis", "basis")

            val parsedTrades = mutableListOf<CryptoTrade>()
            val rejectedTrades = mutableListOf<RejectedTrade>()

            // Process data rows
            lines.drop(1).forEachIndexed { lineIdx, line ->
                val rowIndex = lineIdx + 2 // 1-based index (row 1 is header)
                val tokens = parseCsvLine(line)
                val rawRowMap = headerTokens.mapIndexed { idx, header ->
                    header to tokens.getOrElse(idx) { "" }
                }.toMap()

                val parseResult = parseRow(
                    rowIndex = rowIndex,
                    tokens = tokens,
                    rawRowMap = rawRowMap,
                    columnMap = columnMap,
                    tradeIdIndex = tradeIdIndex,
                    totalIndex = totalIndex,
                    feeIndex = feeIndex,
                    feeCoinIndex = feeCoinIndex,
                    costBasisIndex = costBasisIndex,
                )

                when (parseResult) {
                    is RowParseResult.Success -> parsedTrades.add(parseResult.trade)
                    is RowParseResult.Rejected -> rejectedTrades.add(parseResult.rejectedTrade)
                }
            }

            // Check duplicates against database
            val existingTrades = try {
                cryptoRepository.getExistingTrades()
            } catch (_: Exception) {
                emptyList()
            }

            val existingTradeIds = existingTrades.asSequence()
                .map { it.tradeId.trim() }
                .filter { it.isNotEmpty() }
                .toSet()

            val existingTuples = existingTrades.asSequence().map {
                DuplicateKey(date = it.date, pair = it.pair.uppercase(Locale.US), amount = it.amount)
            }.toSet()

            val readTrades = mutableListOf<CryptoTrade>()
            val duplicateTrades = mutableListOf<CryptoTrade>()

            for (trade in parsedTrades) {
                val key = DuplicateKey(date = trade.date, pair = trade.pair.uppercase(Locale.US), amount = trade.amount)
                val isDuplicateById = trade.tradeId.isNotBlank() && existingTradeIds.contains(trade.tradeId)
                val isDuplicateByTuple = existingTuples.contains(key)

                if (isDuplicateById || isDuplicateByTuple) {
                    duplicateTrades.add(trade)
                } else {
                    readTrades.add(trade)
                }
            }

            Result.success(
                CsvParseResult(
                    read = readTrades,
                    rejected = rejectedTrades,
                    duplicates = duplicateTrades,
                )
            )
        } catch (e: InvalidCsvHeaderException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(AppException("Failed to parse Binance CSV file: ${e.message}", e))
        }
    }

    private sealed class RowParseResult {
        data class Success(val trade: CryptoTrade) : RowParseResult()
        data class Rejected(val rejectedTrade: RejectedTrade) : RowParseResult()
    }

    private fun parseRow(
        rowIndex: Int,
        tokens: List<String>,
        rawRowMap: Map<String, String>,
        columnMap: Map<String, Int>,
        tradeIdIndex: Int?,
        totalIndex: Int?,
        feeIndex: Int?,
        feeCoinIndex: Int?,
        costBasisIndex: Int?,
    ): RowParseResult {
        // Date parsing
        val rawDate = tokens.getOrNull(columnMap["Date"] ?: -1).orEmpty().trim()
        val date = parseDate(rawDate)
            ?: return RowParseResult.Rejected(RejectedTrade(rowIndex, rawRowMap, "Invalid date format"))

        // Pair parsing
        val pair = tokens.getOrNull(columnMap["Pair"] ?: -1).orEmpty().trim()
        if (pair.isEmpty()) {
            return RowParseResult.Rejected(RejectedTrade(rowIndex, rawRowMap, "Missing pair"))
        }

        // Type / Side parsing
        val rawType = tokens.getOrNull(columnMap["Type"] ?: -1).orEmpty().trim()
        val type = parseTradeSide(rawType)
            ?: return RowParseResult.Rejected(RejectedTrade(rowIndex, rawRowMap, "Unknown trade side"))

        // Price parsing
        val rawPrice = tokens.getOrNull(columnMap["Price"] ?: -1).orEmpty().trim()
        val price = rawPrice.toDoubleOrNull()
        if (price == null || (price < 0.0)) {
            return RowParseResult.Rejected(RejectedTrade(rowIndex, rawRowMap, "Invalid price"))
        }

        // Amount parsing
        val rawAmount = tokens.getOrNull(columnMap["Amount"] ?: -1).orEmpty().trim()
        if (rawAmount.isEmpty()) {
            return RowParseResult.Rejected(RejectedTrade(rowIndex, rawRowMap, "Missing amount"))
        }
        val amount = rawAmount.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            return RowParseResult.Rejected(RejectedTrade(rowIndex, rawRowMap, "Missing amount"))
        }

        // Optional Cost Basis validation
        val rawCostBasis = costBasisIndex?.let { tokens.getOrNull(it) }?.trim()
        val costBasis = if (!rawCostBasis.isNullOrEmpty()) {
            rawCostBasis.toDoubleOrNull()
                ?: return RowParseResult.Rejected(RejectedTrade(rowIndex, rawRowMap, "Unknown cost basis"))
        } else {
            null
        }

        // Optional fields
        val tradeId = tradeIdIndex?.let { tokens.getOrNull(it) }?.trim().orEmpty()
        val rawTotal = totalIndex?.let { tokens.getOrNull(it) }?.trim()
        val total = rawTotal?.toDoubleOrNull() ?: (price * amount)
        val rawFee = feeIndex?.let { tokens.getOrNull(it) }?.trim()
        val fee = rawFee?.toDoubleOrNull() ?: 0.0
        val feeCoin = feeCoinIndex?.let { tokens.getOrNull(it) }?.trim().orEmpty()

        return RowParseResult.Success(
            CryptoTrade(
                tradeId = tradeId,
                date = date,
                pair = pair,
                type = type,
                price = price,
                amount = amount,
                total = total,
                fee = fee,
                feeCoin = feeCoin,
                costBasis = costBasis,
            )
        )
    }

    private fun parseTradeSide(rawType: String): TradeSide? {
        val clean = rawType.uppercase(Locale.US)
        return when {
            clean.contains("BUY") -> TradeSide.BUY
            clean.contains("SELL") -> TradeSide.SELL
            else -> null
        }
    }

    private fun parseDate(rawDate: String): LocalDateTime? {
        val cleaned = rawDate.replace("UTC", "").trim()
        for (formatter in DATE_FORMATTERS) {
            try {
                return LocalDateTime.parse(cleaned, formatter)
            } catch (_: DateTimeParseException) {
                // Continue to next formatter
            }
        }
        return null
    }

    private fun cleanHeaderName(header: String): String {
        return header.trim()
            .lowercase(Locale.US)
            .removeSuffix("(utc)")
            .removeSuffix("(utc+0)")
            .trim()
    }

    private fun findIndex(headerMap: Map<String, Int>, vararg candidates: String): Int? {
        return candidates.firstNotNullOfOrNull { candidate -> headerMap[candidate] }
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            when (val c = line[i]) {
                '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ',' -> {
                    if (!inQuotes) {
                        tokens.add(sb.toString().trim())
                        sb.clear()
                    } else {
                        sb.append(c)
                    }
                }
                else -> {
                    sb.append(c)
                }
            }
            i++
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    private data class DuplicateKey(
        val date: LocalDateTime,
        val pair: String,
        val amount: Double,
    )

    private companion object {
        /**
         * Required logical column groups and their acceptable header aliases in Binance CSV exports.
         */
        val REQUIRED_HEADER_GROUPS: Map<String, List<String>> = mapOf(
            "Date" to listOf("date", "date(utc)", "date(utc+0)", "time"),
            "Pair" to listOf("pair", "market", "symbol"),
            "Type" to listOf("type", "side"),
            "Price" to listOf("price"),
            "Amount" to listOf("amount", "executed", "qty", "quantity"),
        )

        val DATE_FORMATTERS = listOf(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
        )
    }
}
