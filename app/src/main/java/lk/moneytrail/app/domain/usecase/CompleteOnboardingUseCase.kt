package lk.moneytrail.app.domain.usecase

import lk.moneytrail.app.domain.model.Account
import lk.moneytrail.app.domain.model.AppException
import lk.moneytrail.app.domain.model.IncomeSource
import lk.moneytrail.app.domain.model.MoneyPlan
import lk.moneytrail.app.domain.repository.AccountRepository
import lk.moneytrail.app.domain.repository.PlanRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Checks what the user entered during onboarding and saves the accounts, then the
 * money plan. The checks are also used step by step on the onboarding screen, so
 * mistakes are caught on the step where they happen.
 */
class CompleteOnboardingUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val planRepository: PlanRepository
) {
    suspend operator fun invoke(accounts: List<Account>, plan: MoneyPlan, today: LocalDate): Result<Unit> {
        val error = accountsError(accounts)
            ?: salaryError(plan.payDay, plan.baseSalary)
            ?: incomeSourcesError(plan.incomeSources)
            ?: goalError(plan.goal.name, plan.goal.targetAmount, plan.goal.savedAmount, plan.goal.targetDate, today)
        if (error != null) return Result.failure(AppException(error))

        accountRepository.saveAccounts(accounts).onFailure { return Result.failure(it) }
        return planRepository.savePlan(plan)
    }

    companion object {
        const val MAX_AMOUNT = 999_999_999L
        const val MAX_GOAL_NAME_LENGTH = 60

        fun accountsError(accounts: List<Account>): String? =
            if (accounts.isEmpty()) "Pick at least one place where you keep money." else null

        fun salaryError(payDay: Int?, baseSalary: Long?): String? = when {
            payDay == null || payDay !in 1..31 -> "Pick the day your salary usually arrives."
            baseSalary == null -> "Enter your base salary, or 0 if you don't get one."
            baseSalary < 0 || baseSalary > MAX_AMOUNT -> "That salary amount isn't valid."
            else -> null
        }

        fun incomeSourcesError(sources: Set<IncomeSource>): String? =
            if (sources.isEmpty()) "Pick at least one way you earn money." else null

        fun goalError(
            name: String,
            targetAmount: Long?,
            savedAmount: Long,
            targetDate: LocalDate,
            today: LocalDate
        ): String? = when {
            name.isBlank() -> "Give your goal a name."
            name.trim().length > MAX_GOAL_NAME_LENGTH -> "Keep the goal name under $MAX_GOAL_NAME_LENGTH characters."
            targetAmount == null || targetAmount <= 0 -> "Enter how much the goal costs."
            targetAmount > MAX_AMOUNT -> "That goal amount is too large."
            savedAmount < 0 || savedAmount > targetAmount -> "What you've saved can't be more than the goal."
            !targetDate.isAfter(today) -> "Pick a target date in the future."
            else -> null
        }
    }
}
