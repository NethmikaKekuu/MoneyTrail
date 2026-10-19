package lk.moneytrail.app.presentation.entry

/**
 * Keypad rules for the amount on the new entry screen. The amount is kept as the
 * digits typed, in whole rupees: no leading zeros and at most [MAX_DIGITS] digits.
 */
object AmountInput {

    const val MAX_DIGITS = 9

    /** [current] with [key] ("0" to "9", or "00") typed after it. Keys that would break the rules are ignored. */
    fun type(current: String, key: String): String {
        if (current.isEmpty() && key.startsWith("0")) return current
        val next = current + key
        return if (next.length > MAX_DIGITS) current else next
    }

    fun delete(current: String): String = current.dropLast(1)
}
