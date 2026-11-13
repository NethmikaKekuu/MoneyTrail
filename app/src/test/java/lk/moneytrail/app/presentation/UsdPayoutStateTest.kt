package lk.moneytrail.app.presentation

import lk.moneytrail.app.domain.repository.MarketRate
import lk.moneytrail.app.presentation.usd.SuggestedRate
import lk.moneytrail.app.presentation.usd.UsdPayoutState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class UsdPayoutStateTest {

    private val day = LocalDate.of(2026, 10, 8)

    private fun state(suggested: SuggestedRate = SuggestedRate.Found(MarketRate(29_940, day))) =
        UsdPayoutState(usdText = "38.00", date = day, suggested = suggested)

    @Test
    fun rupeesFollowTheSuggestedRate() {
        // 38 x 299.40 = 11,377.20
        assertEquals(11_377L, state().lkr)
        assertEquals("11377", state().lkrText)
        assertTrue(state().isLkrAutomatic)
    }

    @Test
    fun aTypedRateReplacesTheSuggestion() {
        val s = state().copy(isOwnRate = true, ownRateText = "296")
        assertEquals(29_600L, s.marketRate)
        assertEquals(11_248L, s.lkr)
    }

    @Test
    fun typedRupeesOverrideTheRate() {
        val s = state().copy(lkrOverride = "11210")
        assertEquals(11_210L, s.lkr)
        assertEquals(167L, s.fee)
        assertFalse(s.isLkrAutomatic)
    }

    @Test
    fun noRateMeansNoAutomaticRupeesAndNoSaving() {
        val s = state(SuggestedRate.Loading)
        assertEquals(0L, s.lkr)
        assertFalse(s.canSave)
        assertTrue(s.copy(lkrOverride = "11210").canSave)
    }

    @Test
    fun needsDollarsToSave() {
        assertFalse(state().copy(usdText = "").canSave)
    }
}
