package lk.moneytrail.app.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import lk.moneytrail.app.domain.model.AppException
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryStatus
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.RecurringBill
import lk.moneytrail.app.domain.model.RecurringBills
import lk.moneytrail.app.domain.model.SpendingType
import lk.moneytrail.app.domain.model.dueDateIn
import lk.moneytrail.app.domain.model.monthsToCreate
import lk.moneytrail.app.domain.model.toExpectedEntry
import lk.moneytrail.app.domain.repository.EntryRepository
import lk.moneytrail.app.domain.repository.RecurringBillRepository
import lk.moneytrail.app.domain.usecase.EnsureRecurringBillsUseCase
import lk.moneytrail.app.domain.usecase.SaveRecurringBillUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class RecurringBillsTest {

    private val today = LocalDate.of(2026, 10, 20)
    private val october = YearMonth.of(2026, 10)

    private val netflix = RecurringBill("b1", "Netflix", 1_990, "Secondary bank", "Other", dueDay = 5)

    // ---- the model ----

    @Test
    fun `due day 31 falls on the last day of a shorter month`() {
        val bill = netflix.copy(dueDay = 31)
        assertEquals(LocalDate.of(2026, 2, 28), bill.dueDateIn(YearMonth.of(2026, 2)))
        assertEquals(LocalDate.of(2026, 11, 30), bill.dueDateIn(YearMonth.of(2026, 11)))
        assertEquals(LocalDate.of(2026, 12, 31), bill.dueDateIn(YearMonth.of(2026, 12)))
    }

    @Test
    fun `a bill becomes an expected committed expense on its due day`() {
        val entry = netflix.toExpectedEntry(october)
        assertEquals(EntryType.EXPENSE, entry.type)
        assertEquals(EntryStatus.EXPECTED, entry.status)
        assertEquals(LocalDate.of(2026, 10, 5), entry.date)
        assertEquals(1_990L, entry.amount)
        assertEquals("Secondary bank", entry.account)
        assertEquals("Netflix", entry.note)
        // A bill is committed whatever its category, even "Other", which is discretionary elsewhere.
        assertEquals(SpendingType.COMMITTED, entry.spendingType)
    }

    @Test
    fun `a new bill is due for this month only`() {
        assertEquals(listOf(october), netflix.monthsToCreate(today))
    }

    @Test
    fun `a bill already made for this month has nothing to create`() {
        assertTrue(netflix.copy(lastCreatedMonth = october).monthsToCreate(today).isEmpty())
    }

    @Test
    fun `a stopped bill has nothing to create`() {
        assertTrue(netflix.copy(isActive = false).monthsToCreate(today).isEmpty())
    }

    @Test
    fun `months missed while away are made up but only the last few`() {
        val longAway = netflix.copy(lastCreatedMonth = YearMonth.of(2026, 3))
        assertEquals(
            listOf(YearMonth.of(2026, 8), YearMonth.of(2026, 9), october),
            longAway.monthsToCreate(today)
        )
        val justMissedOne = netflix.copy(lastCreatedMonth = YearMonth.of(2026, 9))
        assertEquals(listOf(october), justMissedOne.monthsToCreate(today))
    }

    @Test
    fun `a bill is checked before it is saved`() {
        assertEquals("Enter a name for the bill.", RecurringBills.billError(netflix.copy(name = " ")))
        assertEquals("Enter the amount.", RecurringBills.billError(netflix.copy(amount = 0)))
        assertEquals("The due day must be from 1 to 31.", RecurringBills.billError(netflix.copy(dueDay = 32)))
        assertEquals("The due day must be from 1 to 31.", RecurringBills.billError(netflix.copy(dueDay = 0)))
        assertEquals("Pick a category.", RecurringBills.billError(netflix.copy(category = "")))
        assertNull(RecurringBills.billError(netflix))
    }

    // ---- fakes ----

    private class FakeBillRepository(initial: List<RecurringBill>) : RecurringBillRepository {
        val bills = MutableStateFlow(initial)
        var failMarking = false

        override fun observeBills(): Flow<Result<List<RecurringBill>>> = flowOf(Result.success(bills.value))

        override suspend fun saveBill(bill: RecurringBill): Result<Unit> {
            val id = bill.id.ifEmpty { "new-${bills.value.size}" }
            bills.value = bills.value.filterNot { it.id == id } + bill.copy(id = id)
            return Result.success(Unit)
        }

        override suspend fun deleteBill(id: String): Result<Unit> {
            bills.value = bills.value.filterNot { it.id == id }
            return Result.success(Unit)
        }

        override suspend fun markCreated(id: String, month: YearMonth?): Result<Unit> {
            if (failMarking) return Result.failure(AppException("offline"))
            bills.value = bills.value.map { if (it.id == id) it.copy(lastCreatedMonth = month) else it }
            return Result.success(Unit)
        }
    }

    private class FakeEntryRepository : EntryRepository {
        val entries = mutableListOf<Entry>()
        var failAdding = false
        var failAfter = Int.MAX_VALUE

        override fun observeBetween(from: LocalDate, to: LocalDate): Flow<Result<List<Entry>>> =
            flowOf(Result.success(entries.toList()))

        override fun observeExpected(): Flow<Result<List<Entry>>> =
            flowOf(Result.success(entries.filter { it.isExpected }))

        override suspend fun getEntry(id: String): Result<Entry?> = Result.success(entries.find { it.id == id })

        override suspend fun addEntry(entry: Entry): Result<Unit> {
            if (failAdding || entries.size >= failAfter) return Result.failure(AppException("couldn't save"))
            entries += entry.copy(id = "e${entries.size}")
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

        override suspend fun lastLoggedDate(): Result<LocalDate?> = Result.success(null)
    }

    private fun ensure(bills: FakeBillRepository, entries: FakeEntryRepository) =
        EnsureRecurringBillsUseCase(bills, entries)

    // ---- putting bills on the calendar ----

    @Test
    fun `an active bill is put on the calendar once for the month`() = runBlocking {
        val bills = FakeBillRepository(listOf(netflix))
        val entries = FakeEntryRepository()

        assertEquals(1, ensure(bills, entries)(today).getOrThrow())
        assertEquals(1, entries.entries.size)
        assertEquals(LocalDate.of(2026, 10, 5), entries.entries.single().date)
        assertEquals(october, bills.bills.value.single().lastCreatedMonth)
    }

    @Test
    fun `running again in the same month adds nothing`() = runBlocking {
        val bills = FakeBillRepository(listOf(netflix))
        val entries = FakeEntryRepository()
        val ensure = ensure(bills, entries)

        ensure(today)
        assertEquals(0, ensure(today).getOrThrow())
        assertEquals(0, ensure(LocalDate.of(2026, 10, 28)).getOrThrow())
        assertEquals(1, entries.entries.size)
    }

    @Test
    fun `a bill removed with Didn't happen is not offered again that month`() = runBlocking {
        val bills = FakeBillRepository(listOf(netflix))
        val entries = FakeEntryRepository()
        val ensure = ensure(bills, entries)

        ensure(today)
        entries.deleteEntry(entries.entries.single().id)
        ensure(today)

        assertTrue(entries.entries.isEmpty())
    }

    @Test
    fun `a paid bill is not offered again that month but comes back next month`() = runBlocking {
        val bills = FakeBillRepository(listOf(netflix))
        val entries = FakeEntryRepository()
        val ensure = ensure(bills, entries)

        ensure(today)
        val paid = entries.entries.single()
        entries.updateEntry(paid.copy(status = EntryStatus.CONFIRMED))
        ensure(today)
        assertEquals(1, entries.entries.size)

        ensure(LocalDate.of(2026, 11, 2))
        assertEquals(2, entries.entries.size)
        assertEquals(LocalDate.of(2026, 11, 5), entries.entries.last().date)
    }

    @Test
    fun `stopped bills are skipped`() = runBlocking {
        val bills = FakeBillRepository(listOf(netflix.copy(isActive = false)))
        val entries = FakeEntryRepository()

        assertEquals(0, ensure(bills, entries)(today).getOrThrow())
        assertTrue(entries.entries.isEmpty())
    }

    @Test
    fun `a failed entry is tried again next time instead of being lost`() = runBlocking {
        val bills = FakeBillRepository(listOf(netflix))
        val entries = FakeEntryRepository().apply { failAdding = true }
        val ensure = ensure(bills, entries)

        assertTrue(ensure(today).isFailure)
        assertNull(bills.bills.value.single().lastCreatedMonth)

        entries.failAdding = false
        assertEquals(1, ensure(today).getOrThrow())
        assertEquals(1, entries.entries.size)
    }

    @Test
    fun `a failure part-way keeps the months that were added`() = runBlocking {
        val away = netflix.copy(lastCreatedMonth = YearMonth.of(2026, 7))
        val bills = FakeBillRepository(listOf(away))
        val entries = FakeEntryRepository().apply { failAfter = 1 }
        val ensure = ensure(bills, entries)

        // August is added, then September fails: the bill is recorded as done up to August only.
        assertTrue(ensure(today).isFailure)
        assertEquals(1, entries.entries.size)
        assertEquals(YearMonth.of(2026, 8), bills.bills.value.single().lastCreatedMonth)

        entries.failAfter = Int.MAX_VALUE
        assertEquals(2, ensure(today).getOrThrow())
        assertEquals(3, entries.entries.size)
        assertEquals(3, entries.entries.map { it.date }.distinct().size)
    }

    @Test
    fun `no entry is added when the month can't be recorded`() = runBlocking {
        val bills = FakeBillRepository(listOf(netflix)).apply { failMarking = true }
        val entries = FakeEntryRepository()

        assertTrue(ensure(bills, entries)(today).isFailure)
        assertTrue(entries.entries.isEmpty())
    }

    // ---- saving ----

    @Test
    fun `a bill added after its due day this month starts next month`() = runBlocking {
        val bills = FakeBillRepository(emptyList())
        SaveRecurringBillUseCase(bills)(netflix.copy(id = "", dueDay = 5), today).getOrThrow()

        assertEquals(october, bills.bills.value.single().lastCreatedMonth)
        val entries = FakeEntryRepository()
        assertEquals(0, ensure(bills, entries)(today).getOrThrow())
        assertEquals(1, ensure(bills, entries)(LocalDate.of(2026, 11, 1)).getOrThrow())
    }

    @Test
    fun `a bill added before its due day is offered this month`() = runBlocking {
        val bills = FakeBillRepository(emptyList())
        SaveRecurringBillUseCase(bills)(netflix.copy(id = "", dueDay = 25), today).getOrThrow()

        assertNull(bills.bills.value.single().lastCreatedMonth)
        assertEquals(1, ensure(bills, FakeEntryRepository())(today).getOrThrow())
    }

    @Test
    fun `an invalid bill is not saved`() = runBlocking {
        val bills = FakeBillRepository(emptyList())
        val result = SaveRecurringBillUseCase(bills)(netflix.copy(id = "", name = ""), today)

        assertEquals("Enter a name for the bill.", result.exceptionOrNull()?.message)
        assertTrue(bills.bills.value.isEmpty())
    }

    @Test
    fun `editing a bill keeps the month it was last put on the calendar`() = runBlocking {
        val bills = FakeBillRepository(listOf(netflix.copy(lastCreatedMonth = october)))
        SaveRecurringBillUseCase(bills)(bills.bills.value.single().copy(amount = 2_500), today).getOrThrow()

        assertEquals(october, bills.bills.value.single().lastCreatedMonth)
        assertEquals(2_500L, bills.bills.value.single().amount)
    }
}
