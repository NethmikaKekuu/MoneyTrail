package lk.moneytrail.app.presentation.components

import java.util.Locale

/** The typographic minus sign the designs use, which lines up better than a hyphen. */
const val MINUS_SIGN = "−"

/** 2400 -> "2,400". Rupees are shown without decimals, as in the designs. */
fun formatAmount(amount: Long): String = String.format(Locale.US, "%,d", amount)

/** -4030 -> "−4,030", 12000 -> "+12,000", 0 -> "0". */
fun formatNet(amount: Long): String = when {
    amount < 0 -> MINUS_SIGN + formatAmount(-amount)
    amount > 0 -> "+" + formatAmount(amount)
    else -> "0"
}
