package lk.moneytrail.app.presentation

import lk.moneytrail.app.presentation.entry.AmountInput
import org.junit.Assert.assertEquals
import org.junit.Test

class AmountInputTest {

    @Test
    fun typing_buildsTheAmount() {
        val amount = listOf("2", "4", "00").fold("") { current, key -> AmountInput.type(current, key) }

        assertEquals("2400", amount)
    }

    @Test
    fun leadingZeros_areIgnored() {
        assertEquals("", AmountInput.type("", "0"))
        assertEquals("", AmountInput.type("", "00"))
    }

    @Test
    fun amount_stopsAtTheDigitLimit() {
        val full = "9".repeat(AmountInput.MAX_DIGITS)

        assertEquals(full, AmountInput.type(full, "1"))
        assertEquals("1".repeat(8), AmountInput.type("1".repeat(8), "00"))
    }

    @Test
    fun delete_removesTheLastDigit() {
        assertEquals("240", AmountInput.delete("2400"))
        assertEquals("", AmountInput.delete(""))
    }
}
