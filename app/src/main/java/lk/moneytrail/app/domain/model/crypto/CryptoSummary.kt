package lk.moneytrail.app.domain.model.crypto

import java.time.YearMonth

/** One month of closed trades: what selling made or lost after fees. */
data class CryptoMonth(
    val month: YearMonth,
    val realisedPnl: Double,
    val fees: Double,
    val sells: Int
)

/** Realised profit or loss for one trading pair in a year. */
data class CryptoPairTotal(val pair: String, val realisedPnl: Double)

/**
 * What the crypto screen shows for [year]: the year's realised total, each month
 * newest first, and the pairs that made or lost the most. Only sells count; holding
 * a coin that went up or down is not realised.
 */
data class CryptoSummary(
    val year: Int,
    val yearTotal: Double,
    val allTimeTotal: Double,
    val months: List<CryptoMonth>,
    val pairs: List<CryptoPairTotal>,
    val tradeCount: Int
) {
    val isLossYear: Boolean get() = yearTotal < 0
}

fun cryptoSummary(trades: List<CryptoTrade>, monthly: Map<YearMonth, List<CryptoRealisedPnl>>, year: Int): CryptoSummary {
    val all = monthly.values.flatten()
    val thisYear = all.filter { it.sellDate.year == year }
    return CryptoSummary(
        year = year,
        yearTotal = thisYear.sumOf { it.realisedPnl },
        allTimeTotal = all.sumOf { it.realisedPnl },
        months = monthly
            .map { (month, rows) ->
                CryptoMonth(month, rows.sumOf { it.realisedPnl }, rows.sumOf { it.fee }, rows.map { it.sellTradeId to it.sellDate }.distinct().size)
            }
            .sortedByDescending { it.month },
        pairs = thisYear
            .groupBy { it.pair.uppercase() }
            .map { (pair, rows) -> CryptoPairTotal(pair, rows.sumOf { it.realisedPnl }) }
            .sortedByDescending { kotlin.math.abs(it.realisedPnl) },
        tradeCount = trades.size
    )
}

/** "12 trades added · 3 already imported · 1 row skipped". */
fun CsvParseResult.summaryText(): String = buildList {
    add(if (read.size == 1) "1 trade added" else "${read.size} trades added")
    if (duplicates.isNotEmpty()) add("${duplicates.size} already imported")
    if (rejected.isNotEmpty()) add(if (rejected.size == 1) "1 row skipped" else "${rejected.size} rows skipped")
}.joinToString(" · ")
