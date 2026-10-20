package lk.moneytrail.app.domain.usecase

import kotlinx.coroutines.flow.first
import lk.moneytrail.app.domain.model.Accounts
import lk.moneytrail.app.domain.model.Categories
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryStatus
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.IncomeSource
import lk.moneytrail.app.domain.model.payDateIn
import lk.moneytrail.app.domain.repository.AccountRepository
import lk.moneytrail.app.domain.repository.EntryRepository
import lk.moneytrail.app.domain.repository.PlanRepository
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Adds this month's salary as an expected entry on pay day, for the base salary in the
 * user's plan, so it shows on the calendar and gets checked on the day like any other
 * expected money.
 *
 * Does nothing when there's no plan or no salary, when this month already has a
 * Salary entry (expected or confirmed), or once pay day has passed, so a salary the
 * user removed with "Didn't happen" doesn't come back.
 */
class EnsureMonthlySalaryUseCase @Inject constructor(
    private val planRepository: PlanRepository,
    private val entryRepository: EntryRepository,
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(today: LocalDate): Result<Unit> {
        val plan = planRepository.getPlan().getOrElse { return Result.failure(it) }
            ?: return Result.success(Unit)
        if (plan.baseSalary <= 0 || IncomeSource.SALARY !in plan.incomeSources) return Result.success(Unit)

        val month = YearMonth.from(today)
        val payDate = plan.payDateIn(month)
        if (today.isAfter(payDate)) return Result.success(Unit)

        val entries = entryRepository.observeMonth(month).first().getOrElse { return Result.failure(it) }
        if (entries.any { it.type == EntryType.INCOME && it.category == Categories.SALARY }) return Result.success(Unit)

        // The salary account from onboarding comes first; otherwise the default list's first.
        val account = accountRepository.observeAccounts().first().getOrNull()?.firstOrNull()?.name
            ?: Accounts.defaults.first()

        return entryRepository.addEntry(
            Entry(
                id = "",
                type = EntryType.INCOME,
                amount = plan.baseSalary,
                category = Categories.SALARY,
                account = account,
                date = payDate,
                note = "",
                spendingType = null,
                status = EntryStatus.EXPECTED
            )
        )
    }
}
