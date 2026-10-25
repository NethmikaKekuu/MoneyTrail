package lk.moneytrail.app.domain.model

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

enum class IncomeSource { SALARY, FREELANCE, ADSENSE, CRYPTO, OTHER }

data class SavingsGoal(
    val name: String,
    /** In whole rupees (LKR). */
    val targetAmount: Long,
    /** Already put aside before using the app, in LKR. */
    val savedAmount: Long,
    val targetDate: LocalDate,
    val transferDay: Int = 25,
    val allowEmergencyWithdrawals: Boolean = true
)

/**
 * What the user told onboarding about their money: the users/{uid}/settings/plan
 * document in Firestore. Salary reminders, goal progress and the insights read it.
 */
data class MoneyPlan(
    /** Day of the month the salary usually arrives, 1 to 31. */
    val payDay: Int,
    /** The fixed part of the salary before bonuses, in LKR. 0 when there is no salary. */
    val baseSalary: Long,
    val incomeSources: Set<IncomeSource>,
    val goals: List<SavingsGoal>
) {
    /** Primary goal for backwards compatibility */
    val goal: SavingsGoal
        get() = goals.firstOrNull() ?: SavingsGoal("Goal", 0, 0, LocalDate.now())

    /** Secondary constructor for single goal backwards compatibility */
    constructor(
        payDay: Int,
        baseSalary: Long,
        incomeSources: Set<IncomeSource>,
        goal: SavingsGoal
    ) : this(payDay, baseSalary, incomeSources, listOf(goal))
}

/** The day the salary arrives in [month]. A pay day of 31 falls on the last day of shorter months. */
fun MoneyPlan.payDateIn(month: YearMonth): LocalDate =
    month.atDay(payDay.coerceAtMost(month.lengthOfMonth()))

/**
 * LKR to put aside each month to reach the goal by its target month, counting the
 * months from [today]'s month to the target month. A goal due this month or already
 * past needs the whole remainder now. Rounded up, so following it always gets there.
 */
fun SavingsGoal.monthlySavingNeeded(today: LocalDate): Long {
    val remaining = (targetAmount - savedAmount).coerceAtLeast(0)
    val months = ChronoUnit.MONTHS.between(YearMonth.from(today), YearMonth.from(targetDate)).coerceAtLeast(1)
    return (remaining + months - 1) / months
}
