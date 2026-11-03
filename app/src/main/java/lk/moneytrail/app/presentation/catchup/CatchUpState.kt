package lk.moneytrail.app.presentation.catchup

import lk.moneytrail.app.domain.model.Account
import lk.moneytrail.app.domain.model.Entry

/** The three steps of MoneyTrail-Figma-HTML/23-CatchUp.html, then a short "back on track". */
enum class CatchUpStep { CONFIRM, LOG, BALANCE, DONE }

/** One account on the balance step: what the app worked out, and what the user types in. */
data class BalanceRow(
    val account: Account,
    /** What the app thinks the account holds; null before it's ever been balanced. */
    val calculated: Long?,
    val realDigits: String = ""
) {
    val real: Long?
        get() = realDigits.toLongOrNull()

    /** Real minus calculated: below 0 means money left without being logged. Null until both are known. */
    val difference: Long?
        get() = calculated?.let { calc -> real?.let { it - calc } }
}

data class CatchUpState(
    val isLoading: Boolean = true,
    val step: CatchUpStep = CatchUpStep.CONFIRM,
    /** Days since the user last logged something or caught up; null for someone who never logged. */
    val daysAway: Long? = null,
    /** Expected entries that fell due while the user was away, oldest first. */
    val dueEntries: List<Entry> = emptyList(),
    /** Ids the user unticked, so they stay expected. */
    val untickedIds: Set<String> = emptySet(),
    val balanceRows: List<BalanceRow> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    /** After balancing: LKR spent without being logged, and received without being logged. */
    val notLoggedSpent: Long = 0L,
    val notLoggedReceived: Long = 0L,
    /** How many accounts were balanced. */
    val balancedCount: Int = 0,
    /** True once the user is done or said "Later", so the screen can leave. */
    val isFinished: Boolean = false
) {
    val tickedCount: Int
        get() = dueEntries.count { it.id !in untickedIds }

    /** Step number for "step 1 of 3"; the done screen has none. */
    val stepNumber: Int?
        get() = when (step) {
            CatchUpStep.CONFIRM -> 1
            CatchUpStep.LOG -> 2
            CatchUpStep.BALANCE -> 3
            CatchUpStep.DONE -> null
        }
}
