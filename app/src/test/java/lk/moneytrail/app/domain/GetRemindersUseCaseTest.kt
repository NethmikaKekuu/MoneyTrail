package lk.moneytrail.app.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import lk.moneytrail.app.domain.model.Categories
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryStatus
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.IncomeSource
import lk.moneytrail.app.domain.model.MoneyPlan
import lk.moneytrail.app.domain.model.Reminder
import lk.moneytrail.app.domain.model.SavingsGoal
import lk.moneytrail.app.domain.model.SpendingType
import lk.moneytrail.app.domain.repository.CatchUpRepository
import lk.moneytrail.app.domain.repository.EntryRepository
import lk.moneytrail.app.domain.repository.PlanRepository
import lk.moneytrail.app.domain.usecase.CheckCatchUpUseCase
import lk.moneytrail.app.domain.usecase.GetRemindersUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GetRemindersUseCaseTest {

    private class FakeEntryRepository(private val entries: List<Entry>, private val lastLogged: LocalDate?) : EntryRepository {
        override fun observeBetween(from: LocalDate, to: LocalDate): Flow<Result<List<Entry>>> =
            flowOf(Result.success(entries.filter { !it.date.isBefore(from) && !it.date.isAfter(to) }))

        override fun observeExpected(): Flow<Result<List<Entry>>> = flowOf(Result.success(entries.filter { it.isExpected }))

        override suspend fun getEntry(id: String): Result<Entry?> = Result.success(null)
        override suspend fun addEntry(entry: Entry): Result<Unit> = Result.success(Unit)
        override suspend fun updateEntry(entry: Entry): Result<Unit> = Result.success(Unit)
        override suspend fun deleteEntry(id: String): Result<Unit> = Result.success(Unit)
        override suspend fun lastLoggedDate(): Result<LocalDate?> = Result.success(lastLogged)
    }

    private class FakePlanRepository(private val plan: MoneyPlan?) : PlanRepository {
        override suspend fun savePlan(plan: MoneyPlan): Result<Unit> = Result.success(Unit)
        override suspend fun getPlan(): Result<MoneyPlan?> = Result.success(plan)
        override fun observePlan(): Flow<Result<MoneyPlan?>> = flowOf(Result.success(plan))
    }

    private class FakeCatchUpRepository : CatchUpRepository {
        override fun lastCatchUpOn(): LocalDate? = null
        override fun setLastCatchUpOn(date: LocalDate) = Unit
        override fun snoozedOn(): LocalDate? = null
        override fun setSnoozedOn(date: LocalDate) = Unit
    }

    private val today = LocalDate.of(2026, 10, 25)

    private val kavinduPlan = MoneyPlan(
        payDay = 25,
        baseSalary = 120_000,
        incomeSources = setOf(IncomeSource.SALARY),
        goals = listOf(SavingsGoal("MacBook Pro M4", 490_000, 11_200, LocalDate.of(2027, 10, 25), transferDay = 25, id = "mac"))
    )

    private fun reminders(entries: List<Entry>, plan: MoneyPlan? = kavinduPlan, lastLogged: LocalDate? = today): List<Reminder> =
        runBlocking {
            val entryRepository = FakeEntryRepository(entries, lastLogged)
            GetRemindersUseCase(entryRepository, FakePlanRepository(plan), CheckCatchUpUseCase(entryRepository, FakeCatchUpRepository()))(today)
        }

    private fun salary(status: EntryStatus) =
        Entry("s1", EntryType.INCOME, 132_400, Categories.SALARY, "Main bank", today, "", null, status)

    @Test
    fun aSalaryDueToday_isAMoneyDueReminder() {
        val due = salary(EntryStatus.EXPECTED)

        val result = reminders(listOf(due))

        assertEquals(Reminder.MoneyDue(listOf(due)), result.first())
    }

    @Test
    fun onceTheSalaryIsConfirmed_itSaysToPayYourselfFirst() {
        val result = reminders(listOf(salary(EntryStatus.CONFIRMED)))

        assertEquals(listOf(Reminder.PayYourselfFirst(39_900)), result)
    }

    @Test
    fun afterNineDaysAway_itOffersTheCatchUp() {
        val result = reminders(emptyList(), plan = null, lastLogged = today.minusDays(9))

        assertEquals(listOf(Reminder.CatchUp(9)), result)
    }

    @Test
    fun aQuietDay_hasNoReminders() {
        val coffee = Entry("c1", EntryType.EXPENSE, 850, "Coffee", "Cash wallet", today, "", SpendingType.DISCRETIONARY)

        assertTrue(reminders(listOf(coffee), plan = kavinduPlan.copy(payDay = 28, goals = kavinduPlan.goals.map { it.copy(transferDay = 28) })).isEmpty())
    }
}
