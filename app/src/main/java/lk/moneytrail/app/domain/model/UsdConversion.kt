package lk.moneytrail.app.domain.model

/**
 * The sums behind an AdSense payout. Dollars are kept in cents and rates in hundredths of a
 * rupee per dollar (LKR 301.25 is 30125), so everything stays whole numbers like the rest of the app.
 */
object UsdConversion {

    private val Hundredths = Regex("""\d{1,9}(\.\d{0,2})?""")

    /**
     * Typed text such as "42", "42.5" or "42.50" as hundredths (4250), or null when it isn't a
     * plain number with at most two decimals. Used for both dollars and rates.
     */
    fun parseHundredths(text: String): Long? {
        val trimmed = text.trim()
        if (!Hundredths.matches(trimmed)) return null
        val whole = trimmed.substringBefore('.')
        val fraction = trimmed.substringAfter('.', "")
        return whole.toLong() * 100 + fraction.padEnd(2, '0').toLong()
    }

    /** 4250 -> "42.50"; the same for a rate in hundredths. */
    fun formatHundredths(hundredths: Long): String =
        String.format(java.util.Locale.US, "%d.%02d", hundredths / 100, hundredths % 100)

    /** The rate a payout really got: the rupees received for the dollars earned, rounded. */
    fun effectiveRate(usdCents: Long, lkrReceived: Long): Long =
        if (usdCents <= 0 || lkrReceived <= 0) 0L else (lkrReceived * 10_000 + usdCents / 2) / usdCents

    /** Rupees the payout would have been at [marketRate]. */
    fun lkrAtRate(usdCents: Long, marketRate: Long): Long =
        if (usdCents <= 0 || marketRate <= 0) 0L else (usdCents * marketRate + 5_000) / 10_000

    /** What was lost against [marketRate]; 0 when the payout got the market rate or better. */
    fun fee(usdCents: Long, marketRate: Long, lkrReceived: Long): Long =
        (lkrAtRate(usdCents, marketRate) - lkrReceived).coerceAtLeast(0L)

    /** Largest payout the keypad takes: $9,999,999.99. */
    const val MAX_USD_CENTS = 999_999_999L

    /** What's wrong with an entry's dollar fields, or null when it has none or they make sense. */
    fun error(entry: Entry): String? = when {
        entry.usdCents == 0L && entry.effectiveRate == 0L && entry.feeLkr == 0L -> null
        entry.usdCents <= 0 || entry.effectiveRate < 0 || entry.feeLkr < 0 -> "That dollar amount isn't valid."
        entry.usdCents > MAX_USD_CENTS -> "That dollar amount is too large."
        entry.type != EntryType.INCOME || entry.category != Categories.ADSENSE -> "Only AdSense income can be in dollars."
        else -> null
    }
}
