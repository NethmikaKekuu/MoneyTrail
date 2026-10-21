package lk.moneytrail.app.domain

import lk.moneytrail.app.domain.model.CategoryTotal
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.MoneyPlan
import lk.moneytrail.app.domain.model.SavingsGoal
import lk.moneytrail.app.domain.model.SpendingType
import lk.moneytrail.app.domain.model.categoryTotals
import lk.moneytrail.app.domain.model.spendingSplit
import lk.moneytrail.app.domain.model.totals
import lk.moneytrail.app.presentation.goal.GoalProgressState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class GoalProgressTest {

    private val today = LocalDate.of(2026, 10, 19)

    private fun stateWithGoal(
        target: Long,
        saved: Long,
        transfers: Long,
        targetDate: LocalDate = LocalDate.of(2027, 10, 19)
    ) = GoalProgressState(
        today = today,
        isLoading = false,
        plan = MoneyPlan(
            payDay = 25,
            baseSalary = 120_000,
            incomeSources = emptySet(),
            goal = SavingsGoal("MacBook Pro M4", target, saved, targetDate)
        ),
        transferSavingsTotal = transfers
    )

    @Test
    fun macbookGoalProgress_calculatedAccurately() {
        // Target 490,000, initial saved 11,200, transfer 39,900 -> total saved 51,100 -> ~10.43%
        val state = stateWithGoal(target = 490_000, saved = 11_200, transfers = 39_900)

        assertEquals(51_100L, state.savedAmount)
        assertEquals(438_900L, state.remainingAmount)
        assertEquals(10.428, state.percentage, 0.01)
        assertFalse(state.isGoalReached)
    }

    @Test
    fun percentage_cappedAt100_whenSavedExceedsTarget() {
        val state = stateWithGoal(target = 100_000, saved = 80_000, transfers = 50_000)

        assertEquals(100_000L, state.savedAmount) // Capped at target
        assertEquals(0L, state.remainingAmount)
        assertEquals(100.0, state.percentage, 0.0)
        assertTrue(state.isGoalReached)
    }

    @Test
    fun percentage_safeWhenTargetIsZero() {
        val state = stateWithGoal(target = 0, saved = 0, transfers = 0)

        assertEquals(0.0, state.percentage, 0.0)
        assertEquals(0L, state.monthlySavingNeeded)
    }

    @Test
    fun monthlySavingNeeded_returnsZero_whenGoalReached() {
        val state = stateWithGoal(target = 50_000, saved = 50_000, transfers = 0)

        assertEquals(0L, state.monthlySavingNeeded)
        assertTrue(state.isGoalReached)
    }

    @Test
    fun monthlySavingNeeded_needsWholeRemainder_whenTargetDatePassed() {
        val pastDate = LocalDate.of(2026, 9, 1)
        val state = stateWithGoal(target = 100_000, saved = 20_000, transfers = 10_000, targetDate = pastDate)

        assertEquals(70_000L, state.remainingAmount)
        assertEquals(70_000L, state.monthlySavingNeeded)
    }

    @Test
    fun transferEntries_doNotIncreaseExpenseOrIncomeTotals() {
        val month = YearMonth.of(2026, 10)
        val entries = listOf(
            Entry("1", EntryType.INCOME, 120_000, "Salary", "Main bank", today, "", null),
            Entry("2", EntryType.EXPENSE, 5_000, "Food out", "Main bank", today, "", SpendingType.DISCRETIONARY),
            Entry(
                id = "3",
                type = EntryType.TRANSFER,
                amount = 39_900,
                category = "Transfer",
                account = "Main bank",
                date = today,
                note = "Pay yourself first",
                spendingType = null,
                fromAccountId = "Main bank",
                toAccountId = "Savings"
            )
        )

        // Expense totals must only be 5,000 (transfer 39,900 must be completely ignored)
        val totals = entries.totals()
        assertEquals(120_000L, totals.income)
        assertEquals(5_000L, totals.expenses)

        val split = entries.spendingSplit(month)
        assertEquals(5_000L, split.total)

        val categoryTotals = entries.categoryTotals(EntryType.EXPENSE, month)
        assertEquals(1, categoryTotals.size)
        assertEquals(5_000L, categoryTotals.first().total)
    }
}
