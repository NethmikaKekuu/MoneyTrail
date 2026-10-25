package lk.moneytrail.app.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

// Catch-up (MoneyTrail-Figma-HTML/23-CatchUp.html): Kavindu's Google Sheet died because a
// missed week made the gaps feel too big to fix. After a break the app offers a short way
// back: confirm what fell due, log what he remembers, and balance each account so
// anything he didn't log is still counted (21-MonthlyClose.html).

/** Days away before the app offers to catch up. */
const val CATCH_UP_AFTER_DAYS = 3L

/** Days since the user last logged something or caught up; null when they've never logged anything. */
fun daysAway(today: LocalDate, lastLogged: LocalDate?, lastCatchUp: LocalDate?): Long? {
    val lastActive = listOfNotNull(lastLogged, lastCatchUp).maxOrNull() ?: return null
    return ChronoUnit.DAYS.between(lastActive, today).coerceAtLeast(0)
}

/** True when the user has been away [CATCH_UP_AFTER_DAYS] or more and hasn't said "Later" today. */
fun isCatchUpDue(today: LocalDate, lastLogged: LocalDate?, lastCatchUp: LocalDate?, snoozedOn: LocalDate?): Boolean {
    val away = daysAway(today, lastLogged, lastCatchUp) ?: return false
    return away >= CATCH_UP_AFTER_DAYS && snoozedOn != today
}

/** How [entries] moved [account]'s balance: confirmed income in, confirmed expenses out. */
fun List<Entry>.balanceChange(account: String): Long =
    filter { it.account == account && !it.isExpected }
        .sumOf { if (it.type == EntryType.INCOME) it.amount else -it.amount }

/**
 * What the app thinks the account holds now: its balance at the start of the check day
 * plus everything logged in it from that day on ([entriesSinceCheck]). Null before the
 * account has ever been balanced.
 */
fun Account.calculatedBalance(entriesSinceCheck: List<Entry>): Long? =
    checkedBalance?.let { it + entriesSinceCheck.balanceChange(name) }

/**
 * The balance to remember when the user types [realBalance] for [account] today: what it
 * held before [todaysEntries]. Today's entries are counted again from the check day on, so
 * anything logged today, before or after the balance was typed, counts exactly once.
 */
fun startOfDayBalance(realBalance: Long, account: String, todaysEntries: List<Entry>): Long =
    realBalance - todaysEntries.balanceChange(account)

/**
 * The entry that makes the app match the bank when the real balance is [difference] away
 * from the calculated one: money that left without being logged (an expense) or came in
 * without being logged (income), as "Not logged" on [today]. Null when they already match.
 * Unlogged spending is mostly small everyday buys, so it counts as discretionary.
 */
fun notLoggedEntry(account: String, difference: Long, today: LocalDate): Entry? {
    if (difference == 0L) return null
    val isMissingSpending = difference < 0
    return Entry(
        id = "",
        type = if (isMissingSpending) EntryType.EXPENSE else EntryType.INCOME,
        amount = abs(difference),
        category = Categories.NOT_LOGGED,
        account = account,
        date = today,
        note = "Found when balancing the account",
        spendingType = if (isMissingSpending) SpendingType.DISCRETIONARY else null
    )
}
