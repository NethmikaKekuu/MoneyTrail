package lk.moneytrail.app.presentation.onboarding

import lk.moneytrail.app.domain.model.Account
import lk.moneytrail.app.domain.model.AccountTemplates
import lk.moneytrail.app.domain.model.IncomeSource
import lk.moneytrail.app.domain.model.SavingsGoal
import lk.moneytrail.app.domain.model.monthlySavingNeeded
import java.time.LocalDate

/** The four onboarding steps, in order. Only the first is in the Figma file; the rest follow its style. */
enum class OnboardingStep { MONEY_PLACES, SALARY, INCOME_SOURCES, GOAL }

data class OnboardingState(
    val today: LocalDate,
    val step: OnboardingStep = OnboardingStep.MONEY_PLACES,
    val selectedAccountIds: Set<String> = AccountTemplates.defaultIds,
    /** Day of the month the salary arrives; null until picked. */
    val payDay: Int? = null,
    /** Digits typed for the base salary; empty until typed. */
    val baseSalaryDigits: String = "",
    val incomeSources: Set<IncomeSource> = setOf(IncomeSource.SALARY),
    val goalName: String = "",
    val goalTargetDigits: String = "",
    val goalSavedDigits: String = "",
    val goalDate: LocalDate = today.plusYears(1),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    /** Set once everything is saved; the screen then leaves. */
    val isFinished: Boolean = false
) {
    val stepNumber: Int
        get() = step.ordinal + 1

    val stepCount: Int
        get() = OnboardingStep.entries.size

    val isLastStep: Boolean
        get() = step == OnboardingStep.entries.last()

    val accountChoices: List<Account>
        get() = AccountTemplates.all

    val selectedAccounts: List<Account>
        get() = AccountTemplates.all.filter { it.id in selectedAccountIds }

    val baseSalary: Long?
        get() = baseSalaryDigits.toLongOrNull()

    val goalTarget: Long?
        get() = goalTargetDigits.toLongOrNull()

    val goalSaved: Long
        get() = goalSavedDigits.toLongOrNull() ?: 0L

    /** LKR to put aside each month to reach the goal on time; null until a target is typed. */
    val monthlySaving: Long?
        get() = goalTarget?.takeIf { it > 0 }?.let { target ->
            SavingsGoal(goalName, target, goalSaved, goalDate).monthlySavingNeeded(today)
        }
}
