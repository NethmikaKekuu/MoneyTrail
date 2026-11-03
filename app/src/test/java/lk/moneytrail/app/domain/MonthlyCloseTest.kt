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
import lk.moneytrail.app.domain.model.MonthClose
import lk.moneytrail.app.domain.model.SpendingType
import lk.moneytrail.app.domain.model.accountStatements
import lk.moneytrail.app.domain.model.balanceAtEndOf
import lk.moneytrail.app.domain.model.statementTotals
import lk.moneytrail.app.domain.model.toStatementCsv
import lk.moneytrail.app.domain.repository.AccountRepository
import lk.moneytrail.app.domain.repository.EntryRepository
import lk.moneytrail.app.domain.repository.MonthCloseRepository
import lk.moneytrail.app.domain.usecase.CloseMonthUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MonthlyCloseTest {

    private val sep = YearMonth.of(2026, 9)

    // Main bank was balanced at 41,520 at the start of 1 Sep.
    private val main = Account("main-bank", "Main bank", "", AccountKind.BANK, checkedBalance = 41_520, checkedOn = sep.atDay(1))
    private val cash = Account("cash-wallet", "Cash wallet", "", AccountKind.CASH, checkedBalance = 3_000, checkedOn = sep.atDay(1))

    private fun entry(type: EntryType, amount: Long, category: String, account: String, day: Int, note: String = "", expected: Boolean = false) =
        Entry(
            id = "", type = type, amount = amount, category = category, account = account, date = sep.atDay(day), note = note,
            spendingType = if (type == EntryType.EXPENSE) SpendingType.DISCRETIONARY else null,
            status = if (expected) EntryStatus.EXPECTED else EntryStatus.CONFIRMED
        )

    private val entries = listOf(
        entry(EntryType.INCOME, 133_400, Categories.SALARY, "Main bank", 25),
        entry(EntryType.EXPENSE, 34_000, "Rent", "Main bank", 1),
        entry(EntryType.EXPENSE, 2_950, "Coffee", "Cash wallet", 10, note = "Flat white, \"large\""),
        entry(EntryType.EXPENSE, 1_420, Categories.NOT_LOGGED, "Main bank", 30),
        entry(EntryType.INCOME, 20_000, "Freelance", "Main bank", 28, expected = true)
    )

    @Test
    fun balanceAtEndOf_addsWhatWasLoggedSinceTheCheck() {
        assertEquals(41_520L + 133_400 - 34_000 - 1_420, main.balanceAtEndOf(sep.atEndOfMonth(), entries))
        // Checked only in October: nothing to work September out from.
        val late = main.copy(checkedOn = LocalDate.of(2026, 10, 5))
        assertNull(late.balanceAtEndOf(sep.atEndOfMonth(), entries))
    }

    @Test
    fun statementTotals_showNotLoggedApartAndSkipExpected() {
        val totals = entries.statementTotals(sep)
        assertEquals(133_400L, totals.income)
        assertEquals(36_950L, totals.expenses)
        assertEquals(1_420L, totals.unaccounted)
        assertEquals(133_400L - 36_950 - 1_420, totals.kept)
    }

    @Test
    fun accountStatements_openingFromLastMonthsCloseElseFromTheCheck() {
        val fromCheck = accountStatements(sep, listOf(main), entries, previousClose = null).single()
        assertEquals(41_520L, fromCheck.opening)
        assertEquals(133_400L, fromCheck.moneyIn)
        assertEquals(35_420L, fromCheck.moneyOut)
        assertEquals(139_500L, fromCheck.closing)

        val closedAugust = MonthClose(YearMonth.of(2026, 8), mapOf("main-bank" to 40_000), LocalDate.of(2026, 9, 1))
        assertEquals(40_000L, accountStatements(sep, listOf(main), entries, closedAugust).single().opening)
    }

    @Test
    fun csv_hasTheColumnsAndItsAmountsAddUpToWhatWasKept() {
        val csv = entries.toStatementCsv(sep)
        val lines = csv.trimEnd().split("\r\n")
        assertEquals("date,type,category,account,amount,currency,note", lines.first())
        // The expected freelance payment isn't confirmed, so it's left out.
        assertEquals(4, lines.size - 1)
        assertTrue(lines.any { it == "2026-09-10,expense,Coffee,Cash wallet,-2950,LKR,\"Flat white, \"\"large\"\"\"" })

        val sum = lines.drop(1).sumOf { line -> Regex("""^[^,]*,[^,]*,[^,]*,[^,]*,(-?\d+),""").find(line)!!.groupValues[1].toLong() }
        assertEquals(entries.statementTotals(sep).kept, sum)
    }

    // --- closing a month ---

    private class FakeEntryRepository(val entries: MutableList<Entry>) : EntryRepository {
        override fun observeBetween(from: LocalDate, to: LocalDate): Flow<Result<List<Entry>>> =
            flowOf(Result.success(entries.filter { !it.date.isBefore(from) && !it.date.isAfter(to) }))
        override fun observeExpected(): Flow<Result<List<Entry>>> = flowOf(Result.success(emptyList()))
        override suspend fun getEntry(id: String): Result<Entry?> = Result.success(null)
        override suspend fun addEntry(entry: Entry): Result<Unit> = Result.success(Unit).also { entries += entry }
        override suspend fun updateEntry(entry: Entry): Result<Unit> = Result.success(Unit)
        override suspend fun deleteEntry(id: String): Result<Unit> = Result.success(Unit)
        override suspend fun lastLoggedDate(): Result<LocalDate?> = Result.success(null)
    }

    private class FakeAccountRepository : AccountRepository {
        val checks = mutableMapOf<String, Pair<Long, LocalDate>>()
        override fun observeAccounts(): Flow<Result<List<Account>>> = flowOf(Result.success(emptyList()))
        override suspend fun saveAccounts(accounts: List<Account>): Result<Unit> = Result.success(Unit)
        override suspend fun saveBalanceCheck(accountId: String, balance: Long, date: LocalDate): Result<Unit> =
            Result.success(Unit).also { checks[accountId] = balance to date }
    }

    private class FakeCloseRepository : MonthCloseRepository {
        var saved: MonthClose? = null
        override fun observeClose(month: YearMonth): Flow<Result<MonthClose?>> = flowOf(Result.success(saved))
        override suspend fun saveClose(close: MonthClose): Result<Unit> = Result.success(Unit).also { saved = close }
    }

    @Test
    fun closeMonth_addsNotLoggedForTheDifferenceAndSavesTheBalances() = runBlocking {
        val logged = entries.filter { it.category != Categories.NOT_LOGGED }.toMutableList()
        val entryRepository = FakeEntryRepository(logged)
        val accountRepository = FakeAccountRepository()
        val closeRepository = FakeCloseRepository()
        val closeMonth = CloseMonthUseCase(entryRepository, accountRepository, closeRepository)

        // The app works out 140,920 for Main bank; the bank says 139,500. Cash matches.
        val result = closeMonth(sep, listOf(main, cash), mapOf("main-bank" to 139_500L, "cash-wallet" to 50L), LocalDate.of(2026, 10, 1))

        assertTrue(result.isSuccess)
        val notLogged = entryRepository.entries.filter { it.category == Categories.NOT_LOGGED }
        assertEquals(1, notLogged.size)
        assertEquals(EntryType.EXPENSE, notLogged.single().type)
        assertEquals(1_420L, notLogged.single().amount)
        assertEquals(sep.atEndOfMonth(), notLogged.single().date)

        assertEquals(139_500L to LocalDate.of(2026, 10, 1), accountRepository.checks["main-bank"])
        assertEquals(mapOf("main-bank" to 139_500L, "cash-wallet" to 50L), closeRepository.saved?.balances)
    }
}
