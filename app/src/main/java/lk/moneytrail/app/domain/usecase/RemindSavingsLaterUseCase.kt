package lk.moneytrail.app.domain.usecase

import lk.moneytrail.app.domain.model.AppException
import lk.moneytrail.app.domain.repository.PlanRepository
import java.time.LocalDate
import javax.inject.Inject

/** "Remind me tomorrow": hides the Pay yourself first reminder until the next day. */
class RemindSavingsLaterUseCase @Inject constructor(
    private val planRepository: PlanRepository
) {
    suspend operator fun invoke(today: LocalDate): Result<Unit> {
        val plan = planRepository.getPlan().getOrElse { return Result.failure(it) }
            ?: return Result.failure(AppException(UpdateSalaryUseCase.NO_PLAN_MESSAGE))
        return planRepository.savePlan(plan.copy(savingsRemindOn = today.plusDays(1)))
    }
}
