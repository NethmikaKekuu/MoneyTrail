package lk.moneytrail.app.presentation

import android.content.ContextWrapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import lk.moneytrail.app.domain.model.crypto.CryptoTrade
import lk.moneytrail.app.domain.model.crypto.CsvParseResult
import lk.moneytrail.app.domain.model.crypto.TradeSide
import lk.moneytrail.app.domain.model.crypto.cryptoSummary
import lk.moneytrail.app.domain.model.crypto.summaryText
import lk.moneytrail.app.domain.repository.CryptoRepository
import lk.moneytrail.app.domain.usecase.crypto.CalculateRealisedPnlUseCase
import lk.moneytrail.app.domain.usecase.crypto.ImportCryptoTradesUseCase
import lk.moneytrail.app.domain.usecase.crypto.ParseBinanceCsvUseCase
import lk.moneytrail.app.presentation.crypto.formatPnl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class CryptoImportTest {

    /** Keeps trades in memory, like Firestore would. */
    private class FakeCryptoRepository : CryptoRepository {
        val trades = MutableStateFlow<List<CryptoTrade>>(emptyList())
        override suspend fun getExistingTrades(): List<CryptoTrade> = trades.value
        override fun observeTrades(): Flow<Result<List<CryptoTrade>>> = trades.map { Result.success(it) }
        override suspend fun saveTrades(trades: List<CryptoTrade>): Result<Unit> {
            this.trades.value = this.trades.value + trades
            return Result.success(Unit)
        }
        override suspend fun deleteAllTrades(): Result<Unit> {
            trades.value = emptyList()
            return Result.success(Unit)
        }
    }

    private val year = LocalDate.now().year
    private val validCsv = """
        Date(UTC),Pair,Type,Price,Amount,Fee,Fee Coin,Trade ID
        $year-05-10 12:00:00,BTCUSDT,BUY,30000.0,1.0,10.0,USDT,101
        $year-05-15 14:00:00,BTCUSDT,SELL,40000.0,1.0,15.0,USDT,102
    """.trimIndent()

    /** Imports [csv] as if the user had picked a file with that text. */
    private fun importer(csv: String, repo: FakeCryptoRepository): suspend (Unit) -> Result<CsvParseResult> {
        val parser = ParseBinanceCsvUseCase(object : ContextWrapper(null) {}, repo)
        val useCase = ImportCryptoTradesUseCase(parser, repo)
        return { useCase.import { parser.parseInputStream(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8))) } }
    }

    @Test
    fun import_savesTrades_andTheYearsRealisedPnlFollows() = runBlocking {
        val repo = FakeCryptoRepository()
        val result = importer(validCsv, repo)(Unit).getOrThrow()
        assertEquals("2 trades added", result.summaryText())
        assertEquals(2, repo.trades.value.size)
        val summary = cryptoSummary(repo.trades.value, CalculateRealisedPnlUseCase()(repo.trades.value), year)
        assertEquals(9_975.0, summary.yearTotal, 1e-3)
    }

    @Test
    fun importingTheSameFileTwice_addsNothing() = runBlocking {
        val repo = FakeCryptoRepository()
        importer(validCsv, repo)(Unit).getOrThrow()
        val again = importer(validCsv, repo)(Unit).getOrThrow()
        assertEquals("0 trades added · 2 already imported", again.summaryText())
        assertEquals(2, repo.trades.value.size)
    }

    @Test
    fun wrongColumns_saysWhatIsMissing() = runBlocking {
        val error = importer("Date(UTC),Type,Price,Amount\n2023-05-10 12:00:00,BUY,30000.0,1.0", FakeCryptoRepository())(Unit).exceptionOrNull()
        assertTrue(error!!.message!!.contains("Missing: Pair"))
    }

    @Test
    fun noValidRows_saysWhy() = runBlocking {
        val csv = "Date(UTC),Pair,Type,Price,Amount\nnot-a-date,BTCUSDT,BUY,1,1"
        val error = importer(csv, FakeCryptoRepository())(Unit).exceptionOrNull()
        assertEquals("No trades found in that file. Row 2: Invalid date format.", error!!.message)
    }

    @Test
    fun summary_groupsByMonthNewestFirst_andByPair() {
        fun t(id: String, day: String, pair: String, side: TradeSide, price: Double) =
            CryptoTrade(id, LocalDateTime.parse("${day}T10:00:00"), pair, side, price, 1.0, price)
        val trades = listOf(
            t("1", "$year-01-02", "BTCUSDT", TradeSide.BUY, 100.0),
            t("2", "$year-02-02", "BTCUSDT", TradeSide.SELL, 150.0),
            t("3", "$year-03-02", "ETHUSDT", TradeSide.BUY, 50.0),
            t("4", "$year-04-02", "ETHUSDT", TradeSide.SELL, 30.0)
        )
        val summary = cryptoSummary(trades, CalculateRealisedPnlUseCase()(trades), year)
        assertEquals(30.0, summary.yearTotal, 1e-9)
        assertEquals(listOf(YearMonth.of(year, 4), YearMonth.of(year, 2)), summary.months.map { it.month })
        assertEquals(listOf("BTCUSDT" to 50.0, "ETHUSDT" to -20.0), summary.pairs.map { it.pair to it.realisedPnl })
        assertEquals(4, summary.tradeCount)
    }

    @Test
    fun summaryText_andPnlFormat() {
        assertEquals("1 trade added · 1 row skipped", CsvParseResult(listOf(dummy()), listOf(lk.moneytrail.app.domain.model.crypto.RejectedTrade(3, emptyMap(), "x")), emptyList()).summaryText())
        assertEquals("+1,234.50", formatPnl(1234.5))
        assertEquals("−80.00", formatPnl(-80.0))
        assertEquals("0.00", formatPnl(0.0))
    }

    private fun dummy() = CryptoTrade("", LocalDateTime.now(), "BTCUSDT", TradeSide.BUY, 1.0, 1.0, 1.0)
}
