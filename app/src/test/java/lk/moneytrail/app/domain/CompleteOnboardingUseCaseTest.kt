package lk.moneytrail.app.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import lk.moneytrail.app.domain.model.Account
import lk.moneytrail.app.domain.model.AccountTemplates
import lk.moneytrail.app.domain.model.AppException
import lk.moneytrail.app.domain.model.IncomeSource
import lk.moneytrail.app.domain.model.MoneyPlan
import lk.moneytrail.app.domain.model.SavingsGoal
import lk.moneytrail.app.domain.repository.AccountRepository
import lk.moneytrail.app.domain.repository.PlanRepository
import lk.moneytrail.app.domain.usecase.CompleteOnboardingUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CompleteOnboardingUseCaseTest {

    /** Keeps what would have been saved, instead of talking to Firestore. */
    private class FakeAccountRepository(private val failSave: Boolean = false) : AccountRepository {
        val saved = mutableListOf<Account>()

        override fun observeAccounts(): Flow<Result<List<Account>>> = emptyFlow()

        override suspend fun saveAccounts(accounts: List<Account>): Result<Unit> {
            if (failSave) return Result.failure(AppException("Couldn't save your accounts."))
            saved += accounts
            return Result.success(Unit)
        }
    }

    private class FakePlanRepository : PlanRepository {
        var saved: MoneyPlan? = null

        override suspend fun savePlan(plan: MoneyPlan): Result<Unit> {
            saved = plan
            return Result.success(Unit)
        }

        override suspend fun getPlan(): Result<MoneyPlan?> = Result.success(saved)
    }

    private val today = LocalDate.of(2026, 10, 19)
    private val accounts = AccountTemplates.all.take(4)
    private val plan = MoneyPlan(
        payDay = 25,
        baseSalary = 120_000,
        incomeSources = setOf(IncomeSource.SALARY, IncomeSource.FREELANCE, IncomeSource.ADSENSE),
        goal = SavingsGoal("MacBook Pro M4", 490_000, 11_200, LocalDate.of(2027, 10, 19))
    )

    @Test
    fun validOnboarding_savesAccountsAndPlan() = runBlocking {
        val accountRepository = FakeAccountRepository()
        val planRepository = FakePlanRepository()

        val result = CompleteOnboardingUseCase(accountRepository, planRepository)(accounts, plan, today)

        assertTrue(result.isSuccess)
        assertEquals(4, accountRepository.saved.size)
        assertEquals(plan, planRepository.saved)
    }

    @Test
    fun noAccounts_isRejectedAndNothingSaved() = runBlocking {
        val accountRepository = FakeAccountRepository()
        val planRepository = FakePlanRepository()

        val result = CompleteOnboardingUseCase(accountRepository, planRepository)(emptyList(), plan, today)

        assertEquals("Pick at least one place where you keep money.", result.exceptionOrNull()?.message)
        assertTrue(accountRepository.saved.isEmpty())
        assertNull(planRepository.saved)
    }

    @Test
    fun failedAccountSave_stopsBeforeThePlan() = runBlocking {
        val planRepository = FakePlanRepository()

        val result = CompleteOnboardingUseCase(FakeAccountRepository(failSave = true), planRepository)(accounts, plan, today)

        assertTrue(result.isFailure)
        assertNull(planRepository.saved)
    }

    @Test
    fun salaryChecks() {
        assertEquals("Pick the day your salary usually arrives.", CompleteOnboardingUseCase.salaryError(null, 120_000))
        assertEquals("Pick the day your salary usually arrives.", CompleteOnboardingUseCase.salaryError(32, 120_000))
        assertNull(CompleteOnboardingUseCase.salaryError(25, 0))
    }

    @Test
    fun goalChecks() {
        val nextYear = LocalDate.of(2027, 10, 19)

        assertEquals("Give your goal a name.", CompleteOnboardingUseCase.goalError(" ", 490_000, 0, nextYear, today))
        assertEquals("Enter how much the goal costs.", CompleteOnboardingUseCase.goalError("MacBook", null, 0, nextYear, today))
        assertEquals(
            "What you've saved can't be more than the goal.",
            CompleteOnboardingUseCase.goalError("MacBook", 490_000, 500_000, nextYear, today)
        )
        assertEquals("Pick a target date in the future.", CompleteOnboardingUseCase.goalError("MacBook", 490_000, 0, today, today))
        assertNull(CompleteOnboardingUseCase.goalError("MacBook Pro M4", 490_000, 11_200, nextYear, today))
    }
}
