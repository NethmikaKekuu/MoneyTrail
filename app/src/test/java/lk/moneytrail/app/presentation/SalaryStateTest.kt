package lk.moneytrail.app.presentation

import lk.moneytrail.app.presentation.salary.SalaryState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SalaryStateTest {

    @Test
    fun salaryState_showsSaveOnlyOnceThePayDayOrSalaryChanges() {
        val saved = SalaryState(payDay = 25, selectedPayDay = 25, baseSalary = 120_000L, baseSalaryDigits = "120000")

        assertFalse(saved.isChanged)
        assertTrue(saved.copy(selectedPayDay = 28).isChanged)
        assertTrue(saved.copy(baseSalaryDigits = "125000").isChanged)
    }

    @Test
    fun salaryState_anEmptySalaryIsTheSameAsZero() {
        val noSalary = SalaryState(payDay = 25, selectedPayDay = 25, baseSalary = 0L, baseSalaryDigits = "")

        assertFalse(noSalary.isChanged)
    }
}
