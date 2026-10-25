package lk.moneytrail.app.presentation

import lk.moneytrail.app.presentation.salary.ConfirmSalaryUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfirmSalaryStateTest {

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
    fun confirmSalaryUiState_emptyAmount_isZeroSoItCantBeConfirmed() {
        val state = ConfirmSalaryUiState(baseSalary = 120_000L, amountCreditedDigits = "")

        assertEquals(0L, state.totalCredited)
    }

    @Test
    fun confirmSalaryUiState_showsSaveOnlyOnceThePayDayOrSalaryChanges() {
        val saved = ConfirmSalaryUiState(payDay = 25, selectedPayDay = 25, baseSalary = 120_000L, baseSalaryDigits = "120000")

        assertFalse(saved.isSalaryChanged)
        assertTrue(saved.copy(selectedPayDay = 28).isSalaryChanged)
        assertTrue(saved.copy(baseSalaryDigits = "125000").isSalaryChanged)
    }
}
