package lk.moneytrail.app.domain

import kotlinx.coroutines.runBlocking
import lk.moneytrail.app.domain.model.crypto.CryptoTrade
import lk.moneytrail.app.domain.model.crypto.InvalidCsvHeaderException
import lk.moneytrail.app.domain.model.crypto.TradeSide
import lk.moneytrail.app.domain.repository.CryptoRepository
import lk.moneytrail.app.domain.usecase.crypto.ParseBinanceCsvUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.LocalDateTime

class ParseBinanceCsvUseCaseTest {

    private class FakeCryptoRepository(
        private val existing: List<CryptoTrade> = emptyList()
    ) : CryptoRepository {
        override suspend fun getExistingTrades(): List<CryptoTrade> = existing
    }

    private fun createUseCase(existingTrades: List<CryptoTrade> = emptyList()): ParseBinanceCsvUseCase {
        val repo = FakeCryptoRepository(existingTrades)
        // Dummy Context wrapper; parseInputStream directly operates on InputStream and Repository.
        val dummyContext = object : android.content.ContextWrapper(null) {}
        return ParseBinanceCsvUseCase(dummyContext, repo)
    }

    private fun stringToStream(csvText: String): InputStream =
        ByteArrayInputStream(csvText.toByteArray(Charsets.UTF_8))

    @Test
    fun missingRequiredHeader_returnsHeaderException() = runBlocking {
        val invalidHeaderCsv = """
            Date(UTC),Type,Price,Amount
            2023-11-04 12:00:00,BUY,35000.0,0.5
        """.trimIndent()

        val useCase = createUseCase()
        val result = useCase.parseInputStream(stringToStream(invalidHeaderCsv))

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is InvalidCsvHeaderException)
        val missing = (exception as InvalidCsvHeaderException).missingColumns
        assertTrue(missing.contains("Pair"))
    }

    @Test
    fun validCsv_parsesTradesSuccessfully() = runBlocking {
        val csv = """
            Date(UTC),Pair,Type,Price,Amount,Fee,Fee Coin,Trade ID
            2023-11-04 12:00:00,BTCUSDT,BUY,35000.0,0.5,17.5,USDT,1001
            2023-11-04 13:00:00,ETHUSDT,SELL,1800.0,2.0,3.6,USDT,1002
        """.trimIndent()

        val useCase = createUseCase()
        val result = useCase.parseInputStream(stringToStream(csv))

        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertEquals(2, parsed.read.size)
        assertEquals(0, parsed.rejected.size)
        assertEquals(0, parsed.duplicates.size)

        val first = parsed.read[0]
        assertEquals("1001", first.tradeId)
        assertEquals(TradeSide.BUY, first.type)
        assertEquals("BTCUSDT", first.pair)
        assertEquals(35000.0, first.price, 0.001)
        assertEquals(0.5, first.amount, 0.001)
    }

    @Test
    fun invalidRows_areAddedToRejectedWithReason() = runBlocking {
        val csv = """
            Date(UTC),Pair,Type,Price,Amount,Cost Basis
            2023-11-04 12:00:00,BTCUSDT,BUY,35000.0,,
            2023-11-04 13:00:00,ETHUSDT,SELL,1800.0,2.0,invalid_basis
            bad_date,SOLUSDT,BUY,50.0,10.0,
        """.trimIndent()

        val useCase = createUseCase()
        val result = useCase.parseInputStream(stringToStream(csv))

        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertEquals(0, parsed.read.size)
        assertEquals(3, parsed.rejected.size)

        assertEquals("Missing amount", parsed.rejected[0].reason)
        assertEquals("Unknown cost basis", parsed.rejected[1].reason)
        assertEquals("Invalid date format", parsed.rejected[2].reason)
    }

    @Test
    fun duplicateTrades_areIdentifiedByTradeIdOrTuple() = runBlocking {
        val existingDate = LocalDateTime.of(2023, 11, 4, 12, 0, 0)
        val existing = listOf(
            CryptoTrade(
                tradeId = "1001",
                date = existingDate,
                pair = "BTCUSDT",
                type = TradeSide.BUY,
                price = 35000.0,
                amount = 0.5,
                total = 17500.0
            ),
            CryptoTrade(
                tradeId = "",
                date = LocalDateTime.of(2023, 11, 4, 14, 0, 0),
                pair = "ADAUSDT",
                type = TradeSide.BUY,
                price = 0.5,
                amount = 1000.0,
                total = 500.0
            )
        )

        val csv = """
            Date(UTC),Pair,Type,Price,Amount,Trade ID
            2023-11-04 12:00:00,BTCUSDT,BUY,35000.0,0.5,1001
            2023-11-04 14:00:00,ADAUSDT,BUY,0.5,1000.0,9999
            2023-11-04 15:00:00,DOTUSDT,BUY,5.0,200.0,1003
        """.trimIndent()

        val useCase = createUseCase(existingTrades = existing)
        val result = useCase.parseInputStream(stringToStream(csv))

        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()

        // 1 new trade ("1003"), 2 duplicate trades ("1001" by ID, "ADAUSDT" by tuple date+pair+amount)
        assertEquals(1, parsed.read.size)
        assertEquals("1003", parsed.read[0].tradeId)

        assertEquals(2, parsed.duplicates.size)
        assertTrue(parsed.duplicates.any { it.tradeId == "1001" })
        assertTrue(parsed.duplicates.any { it.pair == "ADAUSDT" })
    }
}
