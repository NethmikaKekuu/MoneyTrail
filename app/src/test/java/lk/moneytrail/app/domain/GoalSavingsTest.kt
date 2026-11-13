package lk.moneytrail.app.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import lk.moneytrail.app.domain.model.IncomeSource
import lk.moneytrail.app.domain.model.MoneyPlan
import lk.moneytrail.app.domain.model.SavingsGoal
import lk.moneytrail.app.domain.model.isSavingsDue
import lk.moneytrail.app.domain.model.monthlySavings
import lk.moneytrail.app.domain.model.savingsDayIn
import lk.moneytrail.app.domain.model.withSavingsMoved
import lk.moneytrail.app.domain.repository.PlanRepository
import lk.moneytrail.app.domain.usecase.DeleteGoalUseCase
import lk.moneytrail.app.domain.usecase.MoveSavingsUseCase
import lk.moneytrail.app.domain.usecase.RemindSavingsLaterUseCase
import lk.moneytrail.app.domain.usecase.SaveGoalUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class GoalSavingsTest {

    /** Keeps the plan in memory instead of Firestore. */
    private class FakePlanRepository(var plan: MoneyPlan?) : PlanRepository {
        override suspend fun savePlan(plan: MoneyPlan): Result<Unit> {
            this.plan = plan
            return Result.success(Unit)
        }

        override suspend fun getPlan(): Result<MoneyPlan?> = Result.success(plan)

        override fun observePlan(): Flow<Result<MoneyPlan?>> = flowOf(Result.success(plan))
    }

    private val today = LocalDate.of(2026, 10, 19)
    private val october = YearMonth.of(2026, 10)

    private val macBook = SavingsGoal("MacBook Pro M4", 490_000, 11_200, LocalDate.of(2027, 10, 19), transferDay = 25, id = "mac")
    private val emergency = SavingsGoal("Emergency fund", 60_000, 60_000, LocalDate.of(2027, 4, 1), transferDay = 28, id = "fund")

    private fun plan(vararg goals: SavingsGoal) =
        MoneyPlan(payDay = 25, baseSalary = 120_000, incomeSources = setOf(IncomeSource.SALARY), goals = goals.toList())

    @Test
    fun monthlySavings_leaveOutGoalsAlreadyReached() {
        assertEquals(mapOf("mac" to 39_900L), plan(macBook, emergency).monthlySavings(today))
    }

    @Test
    fun savingsDay_isTheEarliestTransferDay() {
        assertEquals(LocalDate.of(2026, 10, 25), plan(macBook, emergency).savingsDayIn(october))
    }

    @Test
    fun savingsAreDue_onTheTransferDayOrOnceTheSalaryIsConfirmed() {
        val plan = plan(macBook)

        assertFalse(plan.isSavingsDue(today, isSalaryConfirmed = false))
        assertTrue(plan.isSavingsDue(today, isSalaryConfirmed = true))
        assertTrue(plan.isSavingsDue(LocalDate.of(2026, 10, 25), isSalaryConfirmed = false))
    }

    @Test
    fun savingsAreNotDue_onceMovedThisMonthOrSnoozed() {
        val payDay = LocalDate.of(2026, 10, 25)

        assertFalse(plan(macBook).copy(savingsMovedMonth = october).isSavingsDue(payDay, isSalaryConfirmed = true))
        assertFalse(plan(macBook).copy(savingsRemindOn = payDay.plusDays(1)).isSavingsDue(payDay, isSalaryConfirmed = true))
        assertTrue(plan(macBook).copy(savingsRemindOn = payDay).isSavingsDue(payDay, isSalaryConfirmed = true))
        // Next month it's due again.
        assertTrue(plan(macBook).copy(savingsMovedMonth = october).isSavingsDue(LocalDate.of(2026, 11, 25), isSalaryConfirmed = false))
    }

    @Test
    fun movingSavings_neverGoesPastAGoalsTarget() {
        val small = SavingsGoal("Headphones", 30_000, 25_000, LocalDate.of(2026, 12, 1), id = "phones")

        val moved = plan(macBook, small).withSavingsMoved(mapOf("mac" to 39_900, "phones" to 10_000), october)

        assertEquals(51_100L, moved.goals[0].savedAmount)
        assertEquals(30_000L, moved.goals[1].savedAmount)
        assertEquals(october, moved.savingsMovedMonth)
    }

    @Test
    fun moveSavings_savesTheGoalsAndReturnsWhatWasAdded() = runBlocking {
        val plans = FakePlanRepository(plan(macBook).copy(savingsRemindOn = today))

        val moved = MoveSavingsUseCase(plans)(mapOf("mac" to 39_900), today)

        assertEquals(39_900L, moved.getOrNull())
        assertEquals(51_100L, plans.plan?.goals?.first()?.savedAmount)
        assertEquals(october, plans.plan?.savingsMovedMonth)
        assertEquals(null, plans.plan?.savingsRemindOn)
    }

    @Test
    fun remindTomorrow_snoozesUntilTheNextDay() = runBlocking {
        val plans = FakePlanRepository(plan(macBook))

        RemindSavingsLaterUseCase(plans)(today)

        assertEquals(today.plusDays(1), plans.plan?.savingsRemindOn)
    }

    @Test
    fun saveGoal_addsANewGoalWithItsOwnId_evenWithTheSameName() = runBlocking {
        val plans = FakePlanRepository(plan(macBook))

        val result = SaveGoalUseCase(plans)(macBook.copy(id = "", targetAmount = 600_000))

        assertTrue(result.isSuccess)
        val goals = plans.plan?.goals.orEmpty()
        assertEquals(2, goals.size)
        assertTrue(goals[1].id.isNotBlank())
        assertNotEquals("mac", goals[1].id)
    }

    @Test
    fun saveGoal_changesTheGoalWithTheSameId() = runBlocking {
        val plans = FakePlanRepository(plan(macBook, emergency))

        SaveGoalUseCase(plans)(macBook.copy(name = "MacBook Air", targetAmount = 350_000))

        assertEquals(listOf("MacBook Air", "Emergency fund"), plans.plan?.goals?.map { it.name })
    }

    @Test
    fun saveGoal_refusesWhatTheFirestoreRulesWouldReject() = runBlocking {
        val plans = FakePlanRepository(plan(macBook))
        val save = SaveGoalUseCase(plans)

        assertTrue(save(macBook.copy(id = "", name = " ")).isFailure)
        assertTrue(save(macBook.copy(id = "", savedAmount = 500_000)).isFailure)
        assertTrue(save(macBook.copy(id = "", targetAmount = 0)).isFailure)
        assertEquals(1, plans.plan?.goals?.size)
    }

    @Test
    fun deleteGoal_keepsTheLastOne() = runBlocking {
        val plans = FakePlanRepository(plan(macBook, emergency))
        val delete = DeleteGoalUseCase(plans)

        assertTrue(delete("fund").isSuccess)
        assertEquals(DeleteGoalUseCase.LAST_GOAL_MESSAGE, delete("mac").exceptionOrNull()?.message)
        assertEquals(listOf("mac"), plans.plan?.goals?.map { it.id })
    }
}
