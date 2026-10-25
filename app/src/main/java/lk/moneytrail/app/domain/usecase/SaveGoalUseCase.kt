package lk.moneytrail.app.domain.usecase

import lk.moneytrail.app.domain.model.AppException
import lk.moneytrail.app.domain.model.SavingsGoal
import lk.moneytrail.app.domain.repository.PlanRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Adds a savings goal to the plan, or changes the one with the same id. The checks match
 * the Firestore rules, so a goal the rules would reject is never sent.
 */
class SaveGoalUseCase @Inject constructor(
    private val planRepository: PlanRepository
) {
    suspend operator fun invoke(goal: SavingsGoal): Result<Unit> {
        goalError(goal)?.let { return Result.failure(AppException(it)) }

        val plan = planRepository.getPlan().getOrElse { return Result.failure(it) }
            ?: return Result.failure(AppException(UpdateSalaryUseCase.NO_PLAN_MESSAGE))
        val isNew = plan.goals.none { it.id == goal.id }
        if (isNew && plan.goals.size >= MAX_GOALS) {
            return Result.failure(AppException("You can have up to $MAX_GOALS goals."))
        }

        val cleaned = goal.copy(name = goal.name.trim())
        val goals = if (isNew) {
            plan.goals + cleaned.copy(id = cleaned.id.ifBlank { "goal-${UUID.randomUUID().toString().take(8)}" })
        } else {
            plan.goals.map { if (it.id == goal.id) cleaned else it }
        }
        return planRepository.savePlan(plan.copy(goals = goals))
    }

    companion object {
        const val MAX_GOALS = 20
        const val MAX_NAME_LENGTH = 60
        const val MAX_AMOUNT = 999_999_999L

        /** Why [goal] can't be saved, as a message for the user, or null when it's fine. */
        fun goalError(goal: SavingsGoal): String? = when {
            goal.name.isBlank() -> "Give the goal a name."
            goal.name.trim().length > MAX_NAME_LENGTH -> "Keep the name to $MAX_NAME_LENGTH characters."
            goal.targetAmount <= 0 || goal.targetAmount > MAX_AMOUNT -> "Enter a target amount."
            goal.savedAmount < 0 -> "The saved amount can't be negative."
            goal.savedAmount > goal.targetAmount -> "The saved amount can't be more than the target."
            goal.transferDay !in 1..31 -> "Pick a transfer day from 1 to 31."
            else -> null
        }
    }
}
