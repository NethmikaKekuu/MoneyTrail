package lk.moneytrail.app.domain.usecase.crypto

import android.net.Uri
import lk.moneytrail.app.domain.model.AppException
import lk.moneytrail.app.domain.model.crypto.CsvParseResult
import lk.moneytrail.app.domain.model.crypto.InvalidCsvHeaderException
import lk.moneytrail.app.domain.repository.CryptoRepository
import javax.inject.Inject

/**
 * Reads a Binance CSV and saves the trades not already saved. Fails with a message
 * the user can act on: wrong columns, no trades, or the save failing.
 */
class ImportCryptoTradesUseCase @Inject constructor(
    private val parseBinanceCsv: ParseBinanceCsvUseCase,
    private val cryptoRepository: CryptoRepository
) {
    suspend operator fun invoke(uri: Uri): Result<CsvParseResult> = import { parseBinanceCsv(uri) }

    /** The steps after reading the file; [parse] reads it. Split out so tests can pass CSV text. */
    internal suspend fun import(parse: suspend () -> Result<CsvParseResult>): Result<CsvParseResult> {
        val parsed = parse().getOrElse { error ->
            val message = when (error) {
                is InvalidCsvHeaderException ->
                    "That isn't a Binance trade history CSV. Missing: ${error.missingColumns.joinToString(", ")}."
                else -> error.message ?: "Couldn't read that file."
            }
            return Result.failure(AppException(message, error))
        }
        if (parsed.read.isEmpty() && parsed.duplicates.isEmpty()) {
            val why = parsed.rejected.firstOrNull()?.let { " Row ${it.rowIndex}: ${it.reason}." }.orEmpty()
            return Result.failure(AppException("No trades found in that file.$why"))
        }
        if (parsed.read.isNotEmpty()) {
            cryptoRepository.saveTrades(parsed.read).onFailure { return Result.failure(it) }
        }
        return Result.success(parsed)
    }
}
