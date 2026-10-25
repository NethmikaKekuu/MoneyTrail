package lk.moneytrail.app.domain.usecase

import kotlinx.coroutines.flow.first
import lk.moneytrail.app.domain.model.AppException
import lk.moneytrail.app.domain.model.Categories
import lk.moneytrail.app.domain.model.EntryStatus
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.IncomeSource
import lk.moneytrail.app.domain.model.payDateIn
import lk.moneytrail.app.domain.repository.EntryRepository
import lk.moneytrail.app.domain.repository.PlanRepository
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Changes the pay day and base salary from Settings, with the same checks as onboarding.
 * This month's salary follows: one still expected moves to the new base and day, keeping
 * any bonus already added to it (or goes, for a salary of 0), and if there's none yet
 * it's added as on any new month. A salary already confirmed is left as it is.
 */
class UpdateSalaryUseCase @Inject constructor(
    private val planRepository: PlanRepository,
    private val entryRepository: EntryRepository,
    private val ensureMonthlySalary: EnsureMonthlySalaryUseCase
) {
    suspend operator fun invoke(payDay: Int?, baseSalary: Long?, today: LocalDate): Result<Unit> {
        val error = CompleteOnboardingUseCase.salaryError(payDay, baseSalary)
        if (error != null || payDay == null || baseSalary == null) {
            return Result.failure(AppException(error ?: "Pick a pay day and enter your salary."))
        }

        val plan = planRepository.getPlan().getOrElse { return Result.failure(it) }
            ?: return Result.failure(AppException(NO_PLAN_MESSAGE))
        // Someone with a salary earns from Salary, so the calendar should expect it.
        val sources = if (baseSalary > 0) plan.incomeSources + IncomeSource.SALARY else plan.incomeSources
        val updated = plan.copy(payDay = payDay, baseSalary = baseSalary, incomeSources = sources)
        planRepository.savePlan(updated).onFailure { return Result.failure(it) }

        val month = YearMonth.from(today)
        val entries = entryRepository.observeMonth(month).first().getOrElse { return Result.failure(it) }
        val salary = entries.find { it.type == EntryType.INCOME && it.category == Categories.SALARY }
        return when {
            salary == null -> ensureMonthlySalary(today)
            salary.status != EntryStatus.EXPECTED -> Result.success(Unit)
            baseSalary == 0L -> entryRepository.deleteEntry(salary.id)
            else -> entryRepository.updateEntry(
                salary.copy(amount = baseSalary + salary.bonus, date = updated.payDateIn(month))
            )
        }
    }

    companion object {
        const val NO_PLAN_MESSAGE = "Set up your salary and goal first."
    }
}
