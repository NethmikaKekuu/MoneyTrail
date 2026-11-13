package lk.moneytrail.app.domain

import lk.moneytrail.app.domain.model.GoalModel
import lk.moneytrail.app.domain.model.SavingsGoal
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class GoalModelTest {

    private val today = LocalDate.of(2026, 10, 19)

    @Test
    fun goalModel_calculatesMacBookGoalProgressAndMonthlySavings() {
        val model = GoalModel(
            name = "MacBook Pro M4",
            targetAmount = 490_000L,
            savedAmount = 11_200L,
            targetDate = LocalDate.of(2027, 10, 19),
            today = today
        )

        assertEquals("MacBook Pro M4", model.name)
        assertEquals(490_000L, model.targetAmount)
        assertEquals(11_200L, model.savedAmount)
        assertEquals(478_800L, model.remainingAmount)
        assertEquals(12L, model.monthsRemaining)
        assertEquals(39_900L, model.monthlySavingNeeded)
        assertEquals("2.3%", model.percentageText)
        assertEquals("12 months remaining until 19 Oct 2027", model.timelineSummary)
        assertEquals("Shortfall LKR 478,800 ÷ 12 months = LKR 39,900/mo", model.formulaExplanation)
    }

    @Test
    fun goalModel_fromSavingsGoal_createsEquivalentModel() {
        val savingsGoal = SavingsGoal("MacBook Pro M4", 490_000L, 11_200L, LocalDate.of(2027, 10, 19))
        val model = GoalModel.fromSavingsGoal(savingsGoal, today)

        assertEquals("MacBook Pro M4", model.name)
        assertEquals(490_000L, model.targetAmount)
        assertEquals(11_200L, model.savedAmount)
        assertEquals(478_800L, model.remainingAmount)
        assertEquals(39_900L, model.monthlySavingNeeded)
    }

    @Test
    fun goalModel_pastItsTargetDate_isOverdueUntilReached() {
        val late = GoalModel("Bike", 100_000L, 40_000L, LocalDate.of(2026, 9, 30), today = today)

        assertEquals(true, late.isOverdue)
        assertEquals(60_000L, late.monthlySavingNeeded)
        assertEquals(false, late.copy(savedAmount = 100_000L).isOverdue)
    }

    @Test
    fun goalModel_fullySaved_needsZeroMonthlySaving() {
        val model = GoalModel(
            name = "MacBook Pro M4",
            targetAmount = 490_000L,
            savedAmount = 490_000L,
            targetDate = LocalDate.of(2027, 10, 19),
            today = today
        )

        assertEquals(0L, model.remainingAmount)
        assertEquals(0L, model.monthlySavingNeeded)
        assertEquals(1.0f, model.progressFraction, 0.001f)
        assertEquals("100.0%", model.percentageText)
    }
}
