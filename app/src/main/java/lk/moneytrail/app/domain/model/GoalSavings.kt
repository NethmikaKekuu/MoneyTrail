package lk.moneytrail.app.domain.model

import java.time.LocalDate
import java.time.YearMonth

// "Pay yourself first" (MoneyTrail-Figma-HTML/14-SavingsTransfer.html): each month,
// once the salary is in, the user moves the goals' monthly saving to their savings
// account and taps "I've moved it", so goal progress follows real money.

/** What to move this month, by goal id: each goal's monthly saving, leaving out goals already reached. */
fun MoneyPlan.monthlySavings(today: LocalDate): Map<String, Long> =
    goals.filterNot { it.isReached }
        .associate { it.id to it.monthlySavingNeeded(today) }
        .filterValues { it > 0 }

/** The day in [month] to move savings: the earliest transfer day of the goals, or the pay day. */
fun MoneyPlan.savingsDayIn(month: YearMonth): LocalDate {
    val day = goals.minOfOrNull { it.transferDay } ?: payDay
    return month.atDay(day.coerceIn(1, month.lengthOfMonth()))
}

/**
 * True when the app should ask the user to pay themselves first: there's something to
 * save, it hasn't been moved this month, it isn't snoozed with "Remind me tomorrow",
 * and either the transfer day has come or this month's salary is already confirmed.
 */
fun MoneyPlan.isSavingsDue(today: LocalDate, isSalaryConfirmed: Boolean): Boolean {
    val month = YearMonth.from(today)
    return monthlySavings(today).isNotEmpty() &&
        savingsMovedMonth != month &&
        (savingsRemindOn == null || !today.isBefore(savingsRemindOn)) &&
        (isSalaryConfirmed || !today.isBefore(savingsDayIn(month)))
}

/**
 * The plan once [amounts] (LKR by goal id) have been moved to savings in [month]. A goal
 * never goes past its target, which is also what the Firestore rules check.
 */
fun MoneyPlan.withSavingsMoved(amounts: Map<String, Long>, month: YearMonth): MoneyPlan =
    copy(
        goals = goals.map { goal ->
            val moved = (amounts[goal.id] ?: 0L).coerceIn(0L, goal.remaining)
            goal.copy(savedAmount = goal.savedAmount + moved)
        },
        savingsMovedMonth = month,
        savingsRemindOn = null
    )
