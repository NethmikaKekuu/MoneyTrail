package lk.moneytrail.app.presentation.crypto

import lk.moneytrail.app.domain.model.crypto.CryptoSummary
import lk.moneytrail.app.domain.model.crypto.RejectedTrade
import java.util.Locale

/** What the last import did, shown until the next one. */
data class ImportResult(
    val summary: String,
    /** The first few skipped rows and why, e.g. "Row 5: Invalid price". */
    val skippedRows: List<String>
)

/**
 * The crypto screen. The summary is worked out from the saved trades, so it is
 * the same after the app restarts and on another phone.
 */
data class CryptoUiState(
    val isLoading: Boolean = true,
    val summary: CryptoSummary? = null,
    val isImporting: Boolean = false,
    val lastImport: ImportResult? = null,
    /** Shown in the error view when the trades can't be loaded. */
    val loadError: String? = null,
    /** One-off message for a snackbar, e.g. a CSV with the wrong columns. */
    val message: String? = null,
    val isDeleteConfirmOpen: Boolean = false
) {
    val isEmpty: Boolean
        get() = !isLoading && loadError == null && (summary == null || summary.tradeCount == 0)
}

fun RejectedTrade.describe(): String = "Row $rowIndex: $reason"

/** "+1,234.50" or "−80.00", in the quote coin (usually USDT). */
fun formatPnl(value: Double): String {
    val sign = when {
        value > 0.005 -> "+"
        value < -0.005 -> "−"
        else -> ""
    }
    return sign + String.format(Locale.US, "%,.2f", kotlin.math.abs(value))
}
