package lk.moneytrail.app.presentation.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Shows the digits typed in an amount field with thousands commas ("48320" as "48,320")
 * while the field itself keeps plain digits. Putting the commas into the value instead
 * moves the cursor behind the last digit once a comma appears, so the next digit lands
 * in the wrong place.
 */
object AmountVisualTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val formatted = digits.toLongOrNull()?.let { formatAmount(it) } ?: digits
        return TransformedText(AnnotatedString(formatted), CommaOffsetMapping(digits.length, formatted))
    }

    /** Moves the cursor between the plain digits and the same digits with commas. */
    private class CommaOffsetMapping(private val digitCount: Int, private val formatted: String) : OffsetMapping {

        // A comma goes before digit j when (digitCount - j) is a multiple of 3, as in 1,234,567.
        override fun originalToTransformed(offset: Int): Int {
            if (formatted.length == digitCount) return offset
            val commas = (1..minOf(offset, digitCount - 1)).count { (digitCount - it) % 3 == 0 }
            return offset + commas
        }

        override fun transformedToOriginal(offset: Int): Int =
            formatted.take(offset.coerceIn(0, formatted.length)).count(Char::isDigit)
    }
}
