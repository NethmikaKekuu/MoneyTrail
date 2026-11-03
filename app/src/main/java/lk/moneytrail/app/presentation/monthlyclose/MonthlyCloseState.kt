package lk.moneytrail.app.presentation.monthlyclose

import lk.moneytrail.app.domain.model.Account
import java.time.YearMonth

/** One account on the close: what the app worked out, and what the user typed. */
data class CloseRow(
    val account: Account,
    /** Null when the account was first checked after this month, so there's nothing to compare. */
    val calculated: Long?,
    /** Plain digits as typed; empty until the user types the real balance. */
    val realDigits: String = ""
) {
    val real: Long?
        get() = realDigits.toLongOrNull()

    /** Real minus calculated: below 0 means money left without being logged. Null until both are known. */
    val difference: Long?
        get() = real?.let { r -> calculated?.let { r - it } }
}

data class MonthlyCloseState(
    val month: YearMonth,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val rows: List<CloseRow> = emptyList(),
    val isSaving: Boolean = false,
    val saveError: String? = null,
    /** Set once the month is closed, to move on to its statement. */
    val isClosed: Boolean = false
) {
    /** Accounts whose real balance is typed and differs from the app's. */
    val offCount: Int
        get() = rows.count { (it.difference ?: 0L) != 0L }

    val canClose: Boolean
        get() = rows.isNotEmpty() && rows.all { it.real != null } && !isSaving
}
