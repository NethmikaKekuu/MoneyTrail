package lk.moneytrail.app.domain.usecase

import lk.moneytrail.app.domain.model.AppException
import lk.moneytrail.app.domain.model.withSavingsMoved
import lk.moneytrail.app.domain.repository.PlanRepository
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * "I've moved it": adds what the user moved to their savings account to each goal (LKR
 * by goal id), never past a goal's target, and marks this month's saving as done.
 * Returns the LKR added in total.
 */
class MoveSavingsUseCase @Inject constructor(
    private val planRepository: PlanRepository
) {
    suspend operator fun invoke(amounts: Map<String, Long>, today: LocalDate): Result<Long> {
        if (amounts.values.none { it > 0 }) return Result.failure(AppException("Enter how much you moved."))

        val plan = planRepository.getPlan().getOrElse { return Result.failure(it) }
            ?: return Result.failure(AppException(UpdateSalaryUseCase.NO_PLAN_MESSAGE))
        val updated = plan.withSavingsMoved(amounts, YearMonth.from(today))
        val moved = updated.goals.sumOf { it.savedAmount } - plan.goals.sumOf { it.savedAmount }
        return planRepository.savePlan(updated).map { moved }
    }
}
