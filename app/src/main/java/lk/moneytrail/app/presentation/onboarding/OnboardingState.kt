package lk.moneytrail.app.presentation.onboarding

import lk.moneytrail.app.domain.model.Account
import lk.moneytrail.app.domain.model.AccountKind
import lk.moneytrail.app.domain.model.AccountTemplates
import lk.moneytrail.app.domain.model.CustomCategory
import lk.moneytrail.app.domain.model.IncomeSource
import lk.moneytrail.app.domain.model.SavingsGoal
import lk.moneytrail.app.domain.model.monthlySavingNeeded
import java.time.LocalDate

/** The onboarding steps, in order. Only the first is in the Figma file; the rest follow its style. */
enum class OnboardingStep { MONEY_PLACES, BALANCES, SALARY, INCOME_SOURCES, GOAL }

data class OnboardingState(
    val today: LocalDate,
    val step: OnboardingStep = OnboardingStep.MONEY_PLACES,
    val selectedAccountIds: Set<String> = AccountTemplates.defaultIds,
    /** Places the user added beyond the listed ones; they're ticked like the others. */
    val customAccounts: List<Account> = emptyList(),
    /** True while the "Add a place" dialog is open. */
    val isAddingPlace: Boolean = false,
    /** Digits typed for what each place holds now, by account id. Every ticked place needs one. */
    val balanceDigits: Map<String, String> = emptyMap(),
    val placeError: String? = null,
    /** Day of the month the salary arrives; null until picked. */
    val payDay: Int? = null,
    /** Digits typed for the base salary; empty until typed. */
    val baseSalaryDigits: String = "",
    val incomeSources: Set<IncomeSource> = setOf(IncomeSource.SALARY),
    /** Ways of earning the user added beyond the listed ones, saved as income categories. */
    val ownIncome: List<CustomCategory> = emptyList(),
    /** Names of the [ownIncome] still ticked. */
    val selectedOwnIncome: Set<String> = emptySet(),
    /** True while the "New income category" dialog is open. */
    val isAddingIncome: Boolean = false,
    val incomeError: String? = null,
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
        get() = AccountTemplates.all + customAccounts

    val selectedAccounts: List<Account>
        get() = accountChoices.filter { it.id in selectedAccountIds }

    /** The ticked places, each with the balance typed for it today as its starting balance. */
    val accountsToSave: List<Account>
        get() = selectedAccounts.map { account ->
            balanceDigits[account.id]?.toLongOrNull()
                ?.let { account.copy(checkedBalance = it, checkedOn = today) }
                ?: account
        }

    /** The balance typed for the Savings place, offered as the goal's "already saved". */
    val savingsBalanceDigits: String
        get() = selectedAccounts.firstOrNull { it.kind == AccountKind.SAVINGS }
            ?.let { balanceDigits[it.id] }
            .orEmpty()

    val selectedOwnIncomeList: List<CustomCategory>
        get() = ownIncome.filter { it.name in selectedOwnIncome }

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
