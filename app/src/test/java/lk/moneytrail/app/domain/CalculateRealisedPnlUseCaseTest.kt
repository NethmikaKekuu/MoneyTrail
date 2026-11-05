package lk.moneytrail.app.domain

import lk.moneytrail.app.domain.model.crypto.CryptoTrade
import lk.moneytrail.app.domain.model.crypto.TradeSide
import lk.moneytrail.app.domain.usecase.crypto.CalculateRealisedPnlUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.YearMonth

class CalculateRealisedPnlUseCaseTest {

    private val calculatePnl = CalculateRealisedPnlUseCase()

    @Test
    fun singleBuyAndSell_calculatesCorrectPnlAndFee() {
        val nov01 = LocalDateTime.of(2023, 11, 1, 10, 0)
        val nov05 = LocalDateTime.of(2023, 11, 5, 14, 0)

        val trades = listOf(
            CryptoTrade(
                tradeId = "buy_1",
                date = nov01,
                pair = "BTCUSDT",
                type = TradeSide.BUY,
                price = 30000.0,
                amount = 1.0,
                total = 30000.0,
                fee = 10.0,
            ),
            CryptoTrade(
                tradeId = "sell_1",
                date = nov05,
                pair = "BTCUSDT",
                type = TradeSide.SELL,
                price = 40000.0,
                amount = 1.0,
                total = 40000.0,
                fee = 15.0,
            ),
        )

        val result = calculatePnl(trades)
        val novKey = YearMonth.of(2023, 11)

        assertNotNull(result[novKey])
        val pnlList = result.getValue(novKey)
        assertEquals(1, pnlList.size)

        val pnl = pnlList[0]
        assertEquals("sell_1", pnl.sellTradeId)
        assertEquals("BTCUSDT", pnl.pair)
        assertEquals(1.0, pnl.amount, 1e-4)
        assertEquals(40000.0, pnl.proceeds, 1e-4)
        assertEquals(30000.0, pnl.costBasis, 1e-4)
        assertEquals(25.0, pnl.fee, 1e-4) // 10 buy fee + 15 sell fee
        assertEquals(9975.0, pnl.realisedPnl, 1e-4) // 40000 - 30000 - 25
    }

    @Test
    fun partialFills_correctlyTracksRemainingBuyLotAndGroupsByMonth() {
        val nov01 = LocalDateTime.of(2023, 11, 1, 10, 0)
        val nov10 = LocalDateTime.of(2023, 11, 10, 12, 0)
        val dec01 = LocalDateTime.of(2023, 12, 1, 16, 0)

        val trades = listOf(
            CryptoTrade(
                tradeId = "buy_eth",
                date = nov01,
                pair = "ETHUSDT",
                type = TradeSide.BUY,
                price = 2000.0,
                amount = 2.0,
                total = 4000.0,
                fee = 20.0, // 10 fee per ETH
            ),
            CryptoTrade(
                tradeId = "sell_eth_1",
                date = nov10,
                pair = "ETHUSDT",
                type = TradeSide.SELL,
                price = 2500.0,
                amount = 1.0,
                total = 2500.0,
                fee = 10.0,
            ),
            CryptoTrade(
                tradeId = "sell_eth_2",
                date = dec01,
                pair = "ETHUSDT",
                type = TradeSide.SELL,
                price = 3000.0,
                amount = 1.0,
                total = 3000.0,
                fee = 12.0,
            ),
        )

        val result = calculatePnl(trades)

        val novKey = YearMonth.of(2023, 11)
        val decKey = YearMonth.of(2023, 12)

        assertNotNull(result[novKey])
        assertNotNull(result[decKey])

        val novPnl = result.getValue(novKey).single()
        assertEquals("sell_eth_1", novPnl.sellTradeId)
        assertEquals(1.0, novPnl.amount, 1e-4)
        assertEquals(2500.0, novPnl.proceeds, 1e-4)
        assertEquals(2000.0, novPnl.costBasis, 1e-4)
        assertEquals(20.0, novPnl.fee, 1e-4) // 10 (half of buy fee) + 10 sell fee
        assertEquals(480.0, novPnl.realisedPnl, 1e-4)

        val decPnl = result.getValue(decKey).single()
        assertEquals("sell_eth_2", decPnl.sellTradeId)
        assertEquals(1.0, decPnl.amount, 1e-4)
        assertEquals(3000.0, decPnl.proceeds, 1e-4)
        assertEquals(2000.0, decPnl.costBasis, 1e-4)
        assertEquals(22.0, decPnl.fee, 1e-4) // 10 (half of buy fee) + 12 sell fee
        assertEquals(978.0, decPnl.realisedPnl, 1e-4)
    }

    @Test
    fun multipleBuyLots_matchedInFifoOrder() {
        val sep01 = LocalDateTime.of(2023, 9, 1, 10, 0)
        val oct01 = LocalDateTime.of(2023, 10, 1, 10, 0)
        val nov01 = LocalDateTime.of(2023, 11, 1, 10, 0)

        val trades = listOf(
            CryptoTrade(
                tradeId = "buy_1",
                date = sep01,
                pair = "BTCUSDT",
                type = TradeSide.BUY,
                price = 20000.0,
                amount = 1.0,
                total = 20000.0,
                fee = 5.0,
            ),
            CryptoTrade(
                tradeId = "buy_2",
                date = oct01,
                pair = "BTCUSDT",
                type = TradeSide.BUY,
                price = 30000.0,
                amount = 1.0,
                total = 30000.0,
                fee = 5.0,
            ),
            CryptoTrade(
                tradeId = "sell_large",
                date = nov01,
                pair = "BTCUSDT",
                type = TradeSide.SELL,
                price = 40000.0,
                amount = 1.5,
                total = 60000.0,
                fee = 15.0, // 10 fee for 1.0 BTC, 5 fee for 0.5 BTC
            ),
        )

        val result = calculatePnl(trades)
        val novKey = YearMonth.of(2023, 11)

        val pnlList = result.getValue(novKey)
        assertEquals(2, pnlList.size)

        // First matched lot (1.0 BTC from buy_1)
        val match1 = pnlList[0]
        assertEquals(1.0, match1.amount, 1e-4)
        assertEquals(20000.0, match1.costBasis, 1e-4)
        assertEquals(40000.0, match1.proceeds, 1e-4)
        assertEquals(15.0, match1.fee, 1e-4) // 5 buy + 10 sell
        assertEquals(19985.0, match1.realisedPnl, 1e-4)

        // Second matched lot (0.5 BTC from buy_2)
        val match2 = pnlList[1]
        assertEquals(0.5, match2.amount, 1e-4)
        assertEquals(15000.0, match2.costBasis, 1e-4) // 0.5 * 30000
        assertEquals(20000.0, match2.proceeds, 1e-4) // 0.5 * 40000
        assertEquals(7.5, match2.fee, 1e-4) // 2.5 buy + 5.0 sell
        assertEquals(4992.5, match2.realisedPnl, 1e-4)
    }
}
