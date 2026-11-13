package lk.moneytrail.app.presentation

import androidx.compose.ui.text.AnnotatedString
import lk.moneytrail.app.presentation.components.AmountVisualTransformation
import org.junit.Assert.assertEquals
import org.junit.Test

class AmountVisualTransformationTest {

    private fun transform(digits: String) = AmountVisualTransformation.filter(AnnotatedString(digits))

    @Test
    fun digits_areShownWithThousandsCommas() {
        assertEquals("1,000", transform("1000").text.text)
        assertEquals("48,320", transform("48320").text.text)
        assertEquals("1,234,567", transform("1234567").text.text)
        assertEquals("", transform("").text.text)
    }

    @Test
    fun cursorAtTheEnd_staysAtTheEnd() {
        // The defect: typing the 4th digit used to leave the cursor before the last digit.
        listOf("1000", "48320", "1234567").forEach { digits ->
            val transformed = transform(digits)
            assertEquals(transformed.text.length, transformed.offsetMapping.originalToTransformed(digits.length))
            assertEquals(digits.length, transformed.offsetMapping.transformedToOriginal(transformed.text.length))
        }
    }

    @Test
    fun cursorInTheMiddle_skipsTheCommas() {
        val mapping = transform("48320").offsetMapping // "48,320"

        assertEquals(1, mapping.originalToTransformed(1)) // 4|8,320
        assertEquals(3, mapping.originalToTransformed(2)) // 48,|320
        assertEquals(2, mapping.transformedToOriginal(3)) // 48|320
    }
}
