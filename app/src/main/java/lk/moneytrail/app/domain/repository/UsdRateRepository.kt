package lk.moneytrail.app.domain.repository

import java.time.LocalDate

/** A market rate of LKR per USD in hundredths (LKR 299.40 is 29940) and the day it is for. */
data class MarketRate(val rate: Long, val date: LocalDate)

/**
 * Looks up the market rate that suggests what an AdSense payout was worth. It is only a
 * suggestion: what the bank really credited is what's saved. Fails with an
 * [lk.moneytrail.app.domain.model.AppException] when no rate can be found, e.g. offline.
 */
interface UsdRateRepository {

    /** The rate for [date], or the newest one when that day isn't published yet. */
    suspend fun rateFor(date: LocalDate): Result<MarketRate>
}
