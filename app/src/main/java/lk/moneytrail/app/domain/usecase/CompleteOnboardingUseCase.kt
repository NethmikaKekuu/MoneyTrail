package lk.moneytrail.app.domain.usecase

import kotlinx.coroutines.flow.first
import lk.moneytrail.app.domain.model.Account
import lk.moneytrail.app.domain.model.AppException
import lk.moneytrail.app.domain.model.CustomCategory
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.IncomeSource
import lk.moneytrail.app.domain.model.MoneyPlan
import lk.moneytrail.app.domain.repository.AccountRepository
import lk.moneytrail.app.domain.repository.CategoryRepository
import lk.moneytrail.app.domain.repository.PlanRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Checks what the user entered during onboarding and saves the accounts, the money
 * plan, then any income types the user added beyond the listed ones. The checks are
 * also used step by step on the onboarding screen, so mistakes are caught on the step
 * where they happen.
 */
class CompleteOnboardingUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val planRepository: PlanRepository,
    private val categoryRepository: CategoryRepository
) {
    /** [ownIncome] are income categories the user named, e.g. "Tuition classes". */
    suspend operator fun invoke(
        accounts: List<Account>,
        plan: MoneyPlan,
        today: LocalDate,
        ownIncome: List<CustomCategory> = emptyList()
    ): Result<Unit> {
        val error = accountsError(accounts)
            ?: salaryError(plan.payDay, plan.baseSalary)
            ?: incomeSourcesError(plan.incomeSources, ownIncome)
            ?: goalError(plan.goal.name, plan.goal.targetAmount, plan.goal.savedAmount, plan.goal.targetDate, today)
            ?: ownIncome.withIndex().firstNotNullOfOrNull { (index, income) ->
                AddCategoryUseCase.categoryError(income.name, EntryType.INCOME, null, ownIncome.take(index))
            }
        if (error != null) return Result.failure(AppException(error))

        accountRepository.saveAccounts(accounts).onFailure { return Result.failure(it) }
        planRepository.savePlan(plan).onFailure { return Result.failure(it) }

        // Someone who skipped setup may have added some of these already from a new entry.
        val saved = categoryRepository.observeCategories().first().getOrNull().orEmpty()
        ownIncome
            .filter { income -> saved.none { it.type == EntryType.INCOME && it.name.equals(income.name.trim(), ignoreCase = true) } }
            .forEach { income ->
                val category = CustomCategory(id = "", name = income.name.trim(), type = EntryType.INCOME, spendingType = null)
                categoryRepository.addCategory(category).onFailure { return Result.failure(it) }
            }
        return Result.success(Unit)
    }

    companion object {
        const val MAX_AMOUNT = 999_999_999L
        const val MAX_GOAL_NAME_LENGTH = 60
        const val MAX_PLACE_NAME_LENGTH = 30

        fun accountsError(accounts: List<Account>): String? =
            if (accounts.isEmpty()) "Pick at least one place where you keep money." else null

        /**
         * Every place needs what it holds now: the start that catch-up compares the bank
         * against later, to find spending that wasn't logged. 0 is fine for an empty one.
         */
        fun balancesError(accounts: List<Account>): String? = when {
            accounts.any { it.checkedBalance == null } -> "Enter what each place holds now. Type 0 if it's empty."
            accounts.any { (it.checkedBalance ?: 0L) > MAX_AMOUNT } -> "That balance isn't valid."
            else -> null
        }

        /** Checks the name of a place the user adds in step 1 against the places already listed. */
        fun placeError(name: String, existing: List<Account>): String? {
            val trimmed = name.trim()
            return when {
                trimmed.isEmpty() -> "Give the place a name."
                trimmed.length > MAX_PLACE_NAME_LENGTH -> "Keep the name under $MAX_PLACE_NAME_LENGTH characters."
                existing.any { it.name.equals(trimmed, ignoreCase = true) } -> "There's already a place called $trimmed."
                else -> null
            }
        }

        fun salaryError(payDay: Int?, baseSalary: Long?): String? = when {
            payDay == null || payDay !in 1..31 -> "Pick the day your salary usually arrives."
            baseSalary == null -> "Enter your base salary, or 0 if you don't get one."
            baseSalary < 0 || baseSalary > MAX_AMOUNT -> "That salary amount isn't valid."
            else -> null
        }

        fun incomeSourcesError(sources: Set<IncomeSource>, ownIncome: List<CustomCategory> = emptyList()): String? =
            if (sources.isEmpty() && ownIncome.isEmpty()) "Pick at least one way you earn money." else null

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
