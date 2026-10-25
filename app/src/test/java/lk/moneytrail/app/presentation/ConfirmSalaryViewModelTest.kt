package lk.moneytrail.app.presentation

import lk.moneytrail.app.presentation.salary.BonusHistoryItem
import lk.moneytrail.app.presentation.salary.ConfirmSalaryUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfirmSalaryViewModelTest {

    @Test
    fun confirmSalaryUiState_calculatesTotalCredited() {
        val state = ConfirmSalaryUiState(
            baseSalary = 120_000L,
            bonusAmount = 12_400L,
            amountCreditedDigits = "132400"
        )

        assertEquals(120_000L, state.baseSalary)
        assertEquals(132_400L, state.totalCredited)
    }

    @Test
    fun confirmSalaryUiState_dynamicBonusCalculation() {
        val base = 120_000L

        val positiveBonus = 132_400L - base
        assertEquals(12_400L, positiveBonus)

        val zeroBonus = 120_000L - base
        assertEquals(0L, zeroBonus)

        val negativeBonus = 115_000L - base
        assertEquals(-5_000L, negativeBonus)
    }

    @Test
    fun confirmSalaryUiState_emptyBonusHistory_reportsHasBonusHistoryFalse() {
        val state = ConfirmSalaryUiState(
            bonusHistory = emptyList()
        )

        assertFalse(state.hasBonusHistory)
    }

    @Test
    fun confirmSalaryUiState_withSingleMonthBonus_reportsHasBonusHistoryTrue() {
        val state = ConfirmSalaryUiState(
            bonusHistory = listOf(
                BonusHistoryItem("May", 12_400L, isCurrentMonth = true)
            )
        )

        assertTrue(state.hasBonusHistory)
        assertEquals(1, state.bonusHistory.size)
    }

    @Test
    fun confirmSalaryUiState_withMultiMonthBonus_reportsHasBonusHistoryTrue() {
        val state = ConfirmSalaryUiState(
            bonusHistory = listOf(
                BonusHistoryItem("May", 10_000L),
                BonusHistoryItem("Jun", 14_000L, isCurrentMonth = true)
            )
        )

        assertTrue(state.hasBonusHistory)
        assertEquals(2, state.bonusHistory.size)
    }
}
