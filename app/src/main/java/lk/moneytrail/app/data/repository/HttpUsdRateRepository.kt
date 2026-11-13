package lk.moneytrail.app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lk.moneytrail.app.domain.model.AppException
import lk.moneytrail.app.domain.repository.MarketRate
import lk.moneytrail.app.domain.repository.UsdRateRepository
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

/**
 * Rates from the free fawazahmed0 currency-api on jsDelivr, which keeps one file per day
 * and so can answer for a past date. If that fails, open.er-api.com's latest rate is used.
 * Neither needs a key. Found rates are kept for the life of the app so changing the date
 * back and forth doesn't ask again.
 */
@Singleton
class HttpUsdRateRepository @Inject constructor() : UsdRateRepository {

    private val cache = HashMap<LocalDate, MarketRate>()

    override suspend fun rateFor(date: LocalDate): Result<MarketRate> = withContext(Dispatchers.IO) {
        synchronized(cache) { cache[date] }?.let { return@withContext Result.success(it) }

        val today = LocalDate.now()
        val found = fromDailyFile(date, today) ?: fromLatest(today)
        if (found == null) {
            Result.failure(AppException(NOT_FOUND_MESSAGE))
        } else {
            synchronized(cache) { cache[date] = found }
            Result.success(found)
        }
    }

    private fun fromDailyFile(date: LocalDate, today: LocalDate): MarketRate? {
        // Today's file may not be out yet and the future has none, so those ask for the newest.
        val version = if (date < today) date.toString() else "latest"
        val body = get("$DAILY_URL@$version/v1/currencies/usd.min.json") ?: return null
        return runCatching {
            val json = JSONObject(body)
            MarketRate(toHundredths(json.getJSONObject("usd").getDouble("lkr")), LocalDate.parse(json.getString("date")))
        }.getOrNull()
    }

    private fun fromLatest(today: LocalDate): MarketRate? {
        val body = get(LATEST_URL) ?: return null
        return runCatching {
            MarketRate(toHundredths(JSONObject(body).getJSONObject("rates").getDouble("LKR")), today)
        }.getOrNull()
    }

    private fun toHundredths(rate: Double): Long = (rate * 100).roundToLong().also { require(it > 0) }

    /** The response body, or null on any failure. */
    private fun get(address: String): String? = runCatching {
        val connection = URL(address).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            if (connection.responseCode != HttpURLConnection.HTTP_OK) null
            else connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }.getOrNull()

    private companion object {
        const val DAILY_URL = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api"
        const val LATEST_URL = "https://open.er-api.com/v6/latest/USD"
        const val TIMEOUT_MS = 8_000
        const val NOT_FOUND_MESSAGE = "Couldn't get the market rate. Type your own."
    }
}
