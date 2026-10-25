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
import lk.moneytrail.app.domain.model.SpendingType
import lk.moneytrail.app.domain.model.balanceChange
import lk.moneytrail.app.domain.model.daysAway
import lk.moneytrail.app.domain.model.isCatchUpDue
import lk.moneytrail.app.domain.model.notLoggedEntry
import lk.moneytrail.app.domain.repository.AccountRepository
import lk.moneytrail.app.domain.repository.CatchUpRepository
import lk.moneytrail.app.domain.repository.EntryRepository
import lk.moneytrail.app.domain.usecase.BalanceAccountUseCase
import lk.moneytrail.app.domain.usecase.CheckCatchUpUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CatchUpTest {

    /** Keeps entries in a list instead of Firestore. */
    private class FakeEntryRepository(initial: List<Entry> = emptyList(), private val lastLogged: LocalDate? = null) : EntryRepository {
        val entries = initial.toMutableList()

        override fun observeBetween(from: LocalDate, to: LocalDate): Flow<Result<List<Entry>>> =
            flowOf(Result.success(entries.filter { !it.date.isBefore(from) && !it.date.isAfter(to) }))

        override fun observeExpected(): Flow<Result<List<Entry>>> = flowOf(Result.success(entries.filter { it.isExpected }))

        override suspend fun getEntry(id: String): Result<Entry?> = Result.success(entries.find { it.id == id })

        override suspend fun addEntry(entry: Entry): Result<Unit> {
            entries += entry
            return Result.success(Unit)
        }

        override suspend fun updateEntry(entry: Entry): Result<Unit> = Result.success(Unit)

        override suspend fun deleteEntry(id: String): Result<Unit> = Result.success(Unit)

        override suspend fun lastLoggedDate(): Result<LocalDate?> = Result.success(lastLogged)
    }

    private class FakeAccountRepository : AccountRepository {
        val checks = mutableMapOf<String, Pair<Long, LocalDate>>()

        override fun observeAccounts(): Flow<Result<List<Account>>> = flowOf(Result.success(emptyList()))

        override suspend fun saveAccounts(accounts: List<Account>): Result<Unit> = Result.success(Unit)

        override suspend fun saveBalanceCheck(accountId: String, balance: Long, date: LocalDate): Result<Unit> {
            checks[accountId] = balance to date
            return Result.success(Unit)
        }
    }

    private class FakeCatchUpRepository(var lastCatchUp: LocalDate? = null, var snoozed: LocalDate? = null) : CatchUpRepository {
        override fun lastCatchUpOn(): LocalDate? = lastCatchUp
        override fun setLastCatchUpOn(date: LocalDate) { lastCatchUp = date }
        override fun snoozedOn(): LocalDate? = snoozed
        override fun setSnoozedOn(date: LocalDate) { snoozed = date }
    }

    private val today = LocalDate.of(2026, 10, 25)

    private fun entry(type: EntryType, amount: Long, account: String, date: LocalDate, status: EntryStatus = EntryStatus.CONFIRMED) =
        Entry("", type, amount, "Other", account, date, "", if (type == EntryType.EXPENSE) SpendingType.DISCRETIONARY else null, status)

    @Test
    fun catchUp_isOfferedAfterThreeDaysAway() {
        assertFalse(isCatchUpDue(today, lastLogged = today.minusDays(2), lastCatchUp = null, snoozedOn = null))
        assertTrue(isCatchUpDue(today, lastLogged = today.minusDays(9), lastCatchUp = null, snoozedOn = null))
        assertEquals(9L, daysAway(today, today.minusDays(9), null))
    }

    @Test
    fun catchUp_isNotOfferedToNewUsersAfterOneOrWhenPutOffToday() {
        assertFalse(isCatchUpDue(today, lastLogged = null, lastCatchUp = null, snoozedOn = null))
        assertFalse(isCatchUpDue(today, lastLogged = today.minusDays(9), lastCatchUp = today.minusDays(1), snoozedOn = null))
        assertFalse(isCatchUpDue(today, lastLogged = today.minusDays(9), lastCatchUp = null, snoozedOn = today))
    }

    @Test
    fun balanceChange_countsConfirmedMoneyInThatAccountOnly() {
        val entries = listOf(
            entry(EntryType.INCOME, 132_400, "Main bank", today),
            entry(EntryType.EXPENSE, 34_000, "Main bank", today),
            entry(EntryType.EXPENSE, 850, "Cash wallet", today),
            entry(EntryType.EXPENSE, 7_400, "Main bank", today, EntryStatus.EXPECTED)
        )

        assertEquals(98_400L, entries.balanceChange("Main bank"))
    }

    @Test
    fun notLoggedEntry_coversMoneyThatLeftOrCameInUnlogged() {
        val spent = notLoggedEntry("Main bank", -1_420, today)
        val received = notLoggedEntry("Main bank", 500, today)

        assertEquals(EntryType.EXPENSE, spent?.type)
        assertEquals(1_420L, spent?.amount)
        assertEquals(Categories.NOT_LOGGED, spent?.category)
        assertEquals(SpendingType.DISCRETIONARY, spent?.spendingType)
        assertEquals(EntryType.INCOME, received?.type)
        assertNull(notLoggedEntry("Main bank", 0, today))
    }

    @Test
    fun balancing_addsWhatWasNotLoggedAndRemembersTheRealBalance() = runBlocking {
        val checked = LocalDate.of(2026, 10, 1)
        val entries = FakeEntryRepository(listOf(entry(EntryType.EXPENSE, 34_000, "Main bank", LocalDate.of(2026, 10, 2))))
        val accounts = FakeAccountRepository()
        val mainBank = Account("main-bank", "Main bank", "", AccountKind.BANK, checkedBalance = 83_740, checkedOn = checked)

        // The app works out 49,740; the bank shows 48,320, so 1,420 left without being logged.
        val difference = BalanceAccountUseCase(entries, accounts)(mainBank, realBalance = 48_320, today = today)

        assertEquals(-1_420L, difference.getOrNull())
        assertEquals(1_420L, entries.entries.last { it.category == Categories.NOT_LOGGED }.amount)
        assertEquals(48_320L to today, accounts.checks["main-bank"])
    }

    @Test
    fun balancingTheFirstTime_onlySetsTheStartingPoint() = runBlocking {
        val entries = FakeEntryRepository()
        val accounts = FakeAccountRepository()
        val wallet = Account("cash-wallet", "Cash wallet", "", AccountKind.CASH)

        val difference = BalanceAccountUseCase(entries, accounts)(wallet, realBalance = 3_600, today = today)

        assertEquals(0L, difference.getOrNull())
        assertTrue(entries.entries.isEmpty())
        assertEquals(3_600L to today, accounts.checks["cash-wallet"])
    }

    @Test
    fun checkCatchUp_givesTheDaysAwayWhenItsDue() = runBlocking {
        val due = CheckCatchUpUseCase(FakeEntryRepository(lastLogged = today.minusDays(9)), FakeCatchUpRepository())
        val putOff = CheckCatchUpUseCase(FakeEntryRepository(lastLogged = today.minusDays(9)), FakeCatchUpRepository(snoozed = today))

        assertEquals(9L, due(today))
        assertNull(putOff(today))
    }
}
