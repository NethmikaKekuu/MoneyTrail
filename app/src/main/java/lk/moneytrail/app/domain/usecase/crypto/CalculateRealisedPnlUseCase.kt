package lk.moneytrail.app.domain.usecase.crypto

import lk.moneytrail.app.domain.model.crypto.CryptoRealisedPnl
import lk.moneytrail.app.domain.model.crypto.CryptoTrade
import lk.moneytrail.app.domain.model.crypto.TradeSide
import java.time.YearMonth
import java.util.ArrayDeque
import java.util.Locale
import javax.inject.Inject

/**
 * Use case that calculates realised Profit and Loss (PnL) for crypto trades using a
 * First-In-First-Out (FIFO) cost basis accounting model.
 *
 * It processes trades chronologically, maintains open BUY inventory lots per trading pair,
 * handles partial fills, deducts proportional trading fees (both buy and sell fees), and
 * groups the resulting PnL items by [YearMonth].
 */
class CalculateRealisedPnlUseCase @Inject constructor() {

    operator fun invoke(trades: List<CryptoTrade>): Map<YearMonth, List<CryptoRealisedPnl>> {
        val sortedTrades = trades.sortedBy { it.date }
        val buyQueues = mutableMapOf<String, ArrayDeque<BuyLot>>()
        val realisedPnlResults = mutableListOf<CryptoRealisedPnl>()

        for (trade in sortedTrades) {
            val pairKey = trade.pair.trim().uppercase(Locale.US)

            when (trade.type) {
                TradeSide.BUY -> {
                    val queue = buyQueues.getOrPut(pairKey) { ArrayDeque() }
                    queue.addLast(
                        BuyLot(
                            price = trade.price,
                            remainingAmount = trade.amount,
                            initialAmount = trade.amount,
                            fee = trade.fee,
                        ),
                    )
                }

                TradeSide.SELL -> {
                    var amountToSell = trade.amount
                    val queue = buyQueues[pairKey]

                    while ((amountToSell > EPSILON) && (!queue.isNullOrEmpty())) {
                        val earliestBuy = queue.first()
                        val matchedAmount = minOf(amountToSell, earliestBuy.remainingAmount)

                        val matchedCostBasis = matchedAmount * earliestBuy.price
                        val matchedBuyFee = if (earliestBuy.initialAmount > EPSILON) {
                            (matchedAmount / earliestBuy.initialAmount) * earliestBuy.fee
                        } else {
                            0.0
                        }

                        val matchedProceeds = matchedAmount * trade.price
                        val matchedSellFee = if (trade.amount > EPSILON) {
                            (matchedAmount / trade.amount) * trade.fee
                        } else {
                            0.0
                        }

                        val totalFee = matchedBuyFee + matchedSellFee
                        val realisedPnl = matchedProceeds - matchedCostBasis - totalFee

                        realisedPnlResults.add(
                            CryptoRealisedPnl(
                                sellTradeId = trade.tradeId,
                                pair = trade.pair,
                                sellDate = trade.date,
                                amount = matchedAmount,
                                sellPrice = trade.price,
                                proceeds = matchedProceeds,
                                costBasis = matchedCostBasis,
                                fee = totalFee,
                                realisedPnl = realisedPnl,
                            ),
                        )

                        earliestBuy.remainingAmount -= matchedAmount
                        amountToSell -= matchedAmount

                        if (earliestBuy.remainingAmount <= EPSILON) {
                            queue.removeFirst()
                        }
                    }

                    // Handle unmatched sell quantity (if sold without prior recorded BUY history)
                    if (amountToSell > EPSILON) {
                        val matchedProceeds = amountToSell * trade.price
                        val matchedSellFee = if (trade.amount > EPSILON) {
                            (amountToSell / trade.amount) * trade.fee
                        } else {
                            0.0
                        }
                        val realisedPnl = matchedProceeds - matchedSellFee

                        realisedPnlResults.add(
                            CryptoRealisedPnl(
                                sellTradeId = trade.tradeId,
                                pair = trade.pair,
                                sellDate = trade.date,
                                amount = amountToSell,
                                sellPrice = trade.price,
                                proceeds = matchedProceeds,
                                costBasis = 0.0,
                                fee = matchedSellFee,
                                realisedPnl = realisedPnl,
                            ),
                        )
                    }
                }
            }
        }

        return realisedPnlResults.groupBy { YearMonth.from(it.sellDate) }
    }

    private class BuyLot(
        val price: Double,
        var remainingAmount: Double,
        val initialAmount: Double,
        val fee: Double,
    )

    private companion object {
        const val EPSILON = 1e-9
    }
}
