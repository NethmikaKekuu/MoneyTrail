package lk.moneytrail.app.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import lk.moneytrail.app.domain.model.Account
import lk.moneytrail.app.domain.model.AccountKind
import lk.moneytrail.app.domain.model.Categories
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryStatus
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.IncomeSource
import lk.moneytrail.app.domain.model.MoneyPlan
import lk.moneytrail.app.domain.model.SavingsGoal
import lk.moneytrail.app.domain.model.confirmed
import lk.moneytrail.app.domain.model.confirmedSalary
import lk.moneytrail.app.domain.repository.AccountRepository
import lk.moneytrail.app.domain.repository.EntryRepository
import lk.moneytrail.app.domain.repository.PlanRepository
import lk.moneytrail.app.domain.usecase.EnsureMonthlySalaryUseCase
import lk.moneytrail.app.domain.usecase.UpdateSalaryUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SalaryUseCasesTest {


    private class FakeEntryRepository(initial: List<Entry> = emptyList()) : EntryRepository {
        val entries = initial.toMutableList()

        override fun observeBetween(from: LocalDate, to: LocalDate): Flow<Result<List<Entry>>> =
            flowOf(Result.success(entries.filter { !it.date.isBefore(from) && !it.date.isAfter(to) }))

        override fun observeExpected(): Flow<Result<List<Entry>>> = flowOf(Result.success(entries.filter { it.isExpected }))

        override suspend fun getEntry(id: String): Result<Entry?> = Result.success(entries.find { it.id == id })

        override suspend fun addEntry(entry: Entry): Result<Unit> {
            entries += entry.copy(id = "new-${entries.size}")
            return Result.success(Unit)
        }





        override suspend fun updateEntry(entry: Entry): Result<Unit> {
            entries.replaceAll { if (it.id == entry.id) entry else it }
            return Result.success(Unit)
        }

        override suspend fun deleteEntry(id: String): Result<Unit> {
            entries.removeAll { it.id == id }
            return Result.success(Unit)
        }
    }

    private class FakePlanRepository(var plan: MoneyPlan?) : PlanRepository {

        override suspend fun savePlan(plan: MoneyPlan): Result<Unit> {
            this.plan = plan
            return Result.success(Unit)
        }

        override suspend fun getPlan(): Result<MoneyPlan?> = Result.success(plan)

        override fun observePlan(): Flow<Result<MoneyPlan?>> = flowOf(Result.success(plan))
    }

    private class FakeAccountRepository : AccountRepository {
        override fun observeAccounts(): Flow<Result<List<Account>>> =
            flowOf(Result.success(listOf(Account("main-bank", "Main bank", "Card and salary account", AccountKind.BANK))))

        override suspend fun saveAccounts(accounts: List<Account>): Result<Unit> = Result.success(Unit)
    }

    private val today = LocalDate.of(2026, 10, 20)
    private val kavinduPlan = MoneyPlan(
        payDay = 25,
        baseSalary = 120_000,
        incomeSources = setOf(IncomeSource.SALARY, IncomeSource.FREELANCE),
        goal = SavingsGoal("MacBook Pro M4", 490_000, 11_200, LocalDate.of(2027, 10, 19))
    )

    private fun salary(status: EntryStatus, date: LocalDate = LocalDate.of(2026, 10, 25)) = Entry(

        id = "s1",
        type = EntryType.INCOME,
        amount = 120_000,
        category = Categories.SALARY,
        account = "Main bank",
        date = date,
        note = "",
        spendingType = null,
        status = status
    )

    private fun ensure(plans: FakePlanRepository, entries: FakeEntryRepository) =
        EnsureMonthlySalaryUseCase(plans, entries, FakeAccountRepository())

    private fun update(plans: FakePlanRepository, entries: FakeEntryRepository) =
        UpdateSalaryUseCase(plans, entries, ensure(plans, entries))

    @Test
    fun ensure_addsTheSalaryAsExpectedOnPayDay() = runBlocking {
        val entries = FakeEntryRepository()

        ensure(FakePlanRepository(kavinduPlan), entries)(today)

        val added = entries.entries.single()
        assertEquals(EntryStatus.EXPECTED, added.status)
        assertEquals(Categories.SALARY, added.category)
        assertEquals(120_000L, added.amount)
        assertEquals(LocalDate.of(2026, 10, 25), added.date)
        assertEquals("Main bank", added.account)
    }

    @Test
    fun ensure_payDay31_fallsOnTheLastDayOfAShortMonth() = runBlocking {
        val entries = FakeEntryRepository()

        ensure(FakePlanRepository(kavinduPlan.copy(payDay = 31)), entries)(LocalDate.of(2026, 11, 3))

        assertEquals(LocalDate.of(2026, 11, 30), entries.entries.single().date)
    }

    @Test
    fun ensure_doesNothingWhenThisMonthAlreadyHasASalary() = runBlocking {
        val entries = FakeEntryRepository(listOf(salary(EntryStatus.CONFIRMED)))

        ensure(FakePlanRepository(kavinduPlan), entries)(today)

        assertEquals(1, entries.entries.size)
    }

    @Test
    fun ensure_doesNothingAfterPayDayOrWithoutASalary() = runBlocking {
        val entries = FakeEntryRepository()

        ensure(FakePlanRepository(kavinduPlan), entries)(LocalDate.of(2026, 10, 26))
        ensure(FakePlanRepository(kavinduPlan.copy(baseSalary = 0)), entries)(today)
        ensure(FakePlanRepository(null), entries)(today)

        assertTrue(entries.entries.isEmpty())
    }

    @Test
    fun update_movesThisMonthsExpectedSalaryToTheNewAmountAndDay() = runBlocking {
        val plans = FakePlanRepository(kavinduPlan)
        val entries = FakeEntryRepository(listOf(salary(EntryStatus.EXPECTED)))

        val result = update(plans, entries)(28, 150_000, today)

        assertTrue(result.isSuccess)
        assertEquals(28, plans.plan?.payDay)
        assertEquals(150_000L, plans.plan?.baseSalary)
        assertEquals(150_000L, entries.entries.single().amount)
        assertEquals(LocalDate.of(2026, 10, 28), entries.entries.single().date)
    }

    @Test
    fun update_keepsABonusAlreadyOnTheExpectedSalary() = runBlocking {
        val entries = FakeEntryRepository(listOf(salary(EntryStatus.EXPECTED).copy(amount = 135_000, bonus = 15_000)))

        update(FakePlanRepository(kavinduPlan), entries)(25, 150_000, today)

        assertEquals(165_000L, entries.entries.single().amount)
        assertEquals(15_000L, entries.entries.single().bonus)
    }

    @Test
    fun update_leavesASalaryAlreadyConfirmed() = runBlocking {
        val entries = FakeEntryRepository(listOf(salary(EntryStatus.CONFIRMED)))

        update(FakePlanRepository(kavinduPlan), entries)(28, 150_000, today)

        assertEquals(120_000L, entries.entries.single().amount)
    }

    @Test
    fun update_salaryOfZero_removesTheExpectedSalary() = runBlocking {
        val entries = FakeEntryRepository(listOf(salary(EntryStatus.EXPECTED)))

        update(FakePlanRepository(kavinduPlan), entries)(25, 0, today)

        assertTrue(entries.entries.isEmpty())
    }

    @Test
    fun update_newSalary_isAddedToIncomeSourcesAndExpected() = runBlocking {
        val plans = FakePlanRepository(kavinduPlan.copy(baseSalary = 0, incomeSources = setOf(IncomeSource.FREELANCE)))
        val entries = FakeEntryRepository()

        update(plans, entries)(25, 100_000, today)

        assertTrue(IncomeSource.SALARY in plans.plan!!.incomeSources)
        assertEquals(100_000L, entries.entries.single().amount)
    }

    @Test
    fun update_isCheckedAndNeedsAPlan() = runBlocking {
        val plans = FakePlanRepository(kavinduPlan)

        val noDay = update(plans, FakeEntryRepository())(null, 150_000, today)
        val noPlan = update(FakePlanRepository(null), FakeEntryRepository())(25, 150_000, today)

        assertEquals("Pick the day your salary usually arrives.", noDay.exceptionOrNull()?.message)
        assertEquals(kavinduPlan, plans.plan)
        assertEquals(UpdateSalaryUseCase.NO_PLAN_MESSAGE, noPlan.exceptionOrNull()?.message)
    }

    @Test
    fun confirmSalary_withBaseAndBonus_createsConfirmedIncomeEntryWithCorrectTotal() = runBlocking {
        val expectedSalary = salary(EntryStatus.EXPECTED, LocalDate.of(2026, 10, 25))
        val confirmed = expectedSalary.confirmedSalary(base = 120_000, bonus = 12_400, date = today)

        assertEquals(132_400L, confirmed.amount)
        assertEquals(12_400L, confirmed.bonus)
        assertEquals(120_000L, confirmed.baseAmount)
        assertEquals(EntryStatus.CONFIRMED, confirmed.status)
        assertEquals(EntryType.INCOME, confirmed.type)
        assertEquals(Categories.SALARY, confirmed.category)
        assertTrue(confirmed.isSalary)
    }

    @Test
    fun confirmSalary_withZeroBonus_createsConfirmedIncomeEntryWithBaseAmountOnly() = runBlocking {
        val expectedSalary = salary(EntryStatus.EXPECTED, LocalDate.of(2026, 10, 25))
        val confirmed = expectedSalary.confirmedSalary(base = 120_000, bonus = 0, date = today)

        assertEquals(120_000L, confirmed.amount)
        assertEquals(0L, confirmed.bonus)
        assertEquals(120_000L, confirmed.baseAmount)
        assertEquals(EntryStatus.CONFIRMED, confirmed.status)
    }

    @Test
    fun confirmIncome_otherSources_createsConfirmedIncomeEntryWithoutBonus() = runBlocking {
        val freelanceIncome = Entry(
            id = "i1",
            type = EntryType.INCOME,
            amount = 45_000,
            category = "Freelance",
            account = "Main bank",
            date = LocalDate.of(2026, 10, 20),
            note = "Project milestone",
            spendingType = null,
            status = EntryStatus.EXPECTED
        )

        val confirmed = freelanceIncome.confirmed(amount = 50_000, date = today)

        assertEquals(50_000L, confirmed.amount)
        assertEquals(0L, confirmed.bonus)
        assertEquals(EntryStatus.CONFIRMED, confirmed.status)
        assertEquals(EntryType.INCOME, confirmed.type)
        assertEquals("Freelance", confirmed.category)
    }
}
