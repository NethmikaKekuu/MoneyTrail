package lk.moneytrail.app.domain

import lk.moneytrail.app.domain.model.UsdConversion
import org.junit.Assert.assertEquals
import org.junit.Test

class UsdConversionTest {

    @Test
    fun effectiveRate_isRupeesReceivedPerDollar() {
        // $42.00 paid out as LKR 12,600 is exactly LKR 300 a dollar.
        assertEquals(30_000L, UsdConversion.effectiveRate(usdCents = 4_200, lkrReceived = 12_600))
    }

    @Test
    fun effectiveRate_roundsToHundredths() {
        // 12,601 / 42 = 300.0238... -> 300.02
        assertEquals(30_002L, UsdConversion.effectiveRate(4_200, 12_601))
    }

    @Test
    fun effectiveRate_isZeroWithoutBothAmounts() {
        assertEquals(0L, UsdConversion.effectiveRate(0, 12_600))
        assertEquals(0L, UsdConversion.effectiveRate(4_200, 0))
    }

    @Test
    fun fee_isTheShortfallAgainstTheMarketRate() {
        // Market 305.00 would give 12,810; 12,600 arrived, so 210 was lost.
        assertEquals(12_810L, UsdConversion.lkrAtRate(4_200, 30_500))
        assertEquals(210L, UsdConversion.fee(4_200, 30_500, 12_600))
    }

    @Test
    fun fee_isNeverNegative() {
        assertEquals(0L, UsdConversion.fee(4_200, 29_000, 12_600))
    }
}

class UsdConversionTextTest {

    @Test
    fun parseHundredths_readsWholeAndDecimalNumbers() {
        assertEquals(4_200L, UsdConversion.parseHundredths("42"))
        assertEquals(4_250L, UsdConversion.parseHundredths("42.5"))
        assertEquals(4_205L, UsdConversion.parseHundredths("42.05"))
        assertEquals(4_200L, UsdConversion.parseHundredths("42."))
    }

    @Test
    fun parseHundredths_rejectsAnythingElse() {
        assertEquals(null, UsdConversion.parseHundredths(""))
        assertEquals(null, UsdConversion.parseHundredths("4.123"))
        assertEquals(null, UsdConversion.parseHundredths("-3"))
        assertEquals(null, UsdConversion.parseHundredths("abc"))
    }

    @Test
    fun formatHundredths_alwaysShowsTwoDecimals() {
        assertEquals("42.05", UsdConversion.formatHundredths(4_205))
        assertEquals("301.00", UsdConversion.formatHundredths(30_100))
    }
}
