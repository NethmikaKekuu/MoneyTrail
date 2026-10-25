package lk.moneytrail.app.presentation

import lk.moneytrail.app.presentation.onboarding.OnboardingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class OnboardingStateTest {

    private val today = LocalDate.of(2026, 10, 25)

    @Test
    fun accountsToSave_carryTheBalancesTypedAsStartingBalances() {
        val state = OnboardingState(
            today = today,
            selectedAccountIds = setOf("main-bank", "cash-wallet"),
            balanceDigits = mapOf("main-bank" to "48320", "cash-wallet" to "")
        )

        val accounts = state.accountsToSave.associateBy { it.id }

        assertEquals(48_320L, accounts["main-bank"]?.checkedBalance)
        assertEquals(today, accounts["main-bank"]?.checkedOn)
        // Not typed yet: the step asks for it before going on.
        assertNull(accounts["cash-wallet"]?.checkedBalance)
    }

    @Test
    fun savingsBalance_isOfferedAsWhatsAlreadySavedForTheGoal() {
        val state = OnboardingState(
            today = today,
            selectedAccountIds = setOf("main-bank", "savings"),
            balanceDigits = mapOf("main-bank" to "48320", "savings" to "11200")
        )

        assertEquals("11200", state.savingsBalanceDigits)
    }

    @Test
    fun accountsToSave_leaveOutBalancesOfPlacesNoLongerTicked() {
        val state = OnboardingState(
            today = today,
            selectedAccountIds = setOf("main-bank"),
            balanceDigits = mapOf("savings" to "11200")
        )

        assertEquals(listOf("main-bank"), state.accountsToSave.map { it.id })
    }
}
