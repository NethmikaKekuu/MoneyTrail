package lk.moneytrail.app.domain

import lk.moneytrail.app.domain.model.SavingsGoal
import lk.moneytrail.app.domain.model.monthlySavingNeeded
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SavingsGoalTest {

    private val today = LocalDate.of(2026, 10, 19)

    private fun goal(target: Long, saved: Long, date: LocalDate) =
        SavingsGoal(name = "MacBook Pro M4", targetAmount = target, savedAmount = saved, targetDate = date)

    @Test
    fun kavinduNeeds39900AMonth() {
        // The scenario: LKR 490,000, 11,200 saved, twelve months to go.
        val monthly = goal(490_000, 11_200, LocalDate.of(2027, 10, 19)).monthlySavingNeeded(today)

        assertEquals(39_900L, monthly)
    }

    @Test
    fun monthlyAmount_isRoundedUpSoTheGoalIsReached() {
        val monthly = goal(100_000, 0, LocalDate.of(2027, 1, 10)).monthlySavingNeeded(today)

        assertEquals(33_334L, monthly)
    }

    @Test
    fun goalDueThisMonth_needsTheWholeRemainderNow() {
        val monthly = goal(50_000, 20_000, LocalDate.of(2026, 10, 31)).monthlySavingNeeded(today)

        assertEquals(30_000L, monthly)
    }

    @Test
    fun goalAlreadySaved_needsNothing() {
        val monthly = goal(50_000, 50_000, LocalDate.of(2027, 6, 1)).monthlySavingNeeded(today)

        assertEquals(0L, monthly)
    }
}
