package lk.moneytrail.app.domain.usecase

import lk.moneytrail.app.domain.model.AppException
import lk.moneytrail.app.domain.repository.PlanRepository
import javax.inject.Inject

/** Removes a goal from the plan. The plan always keeps at least one goal. */
class DeleteGoalUseCase @Inject constructor(
    private val planRepository: PlanRepository
) {
    suspend operator fun invoke(goalId: String): Result<Unit> {
        val plan = planRepository.getPlan().getOrElse { return Result.failure(it) }
            ?: return Result.failure(AppException(UpdateSalaryUseCase.NO_PLAN_MESSAGE))
        val goals = plan.goals.filterNot { it.id == goalId }
        if (goals.size == plan.goals.size) return Result.success(Unit)
        if (goals.isEmpty()) return Result.failure(AppException(LAST_GOAL_MESSAGE))
        return planRepository.savePlan(plan.copy(goals = goals))
    }

    companion object {
        const val LAST_GOAL_MESSAGE = "Keep at least one goal. Add another one before removing this."
    }
}
