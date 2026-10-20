package lk.moneytrail.app.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.SpendingType
import lk.moneytrail.app.domain.repository.EntryRepository
import lk.moneytrail.app.domain.usecase.UpdateEntryUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class UpdateEntryUseCaseTest {

    /** Keeps the last update instead of talking to Firestore. */
    private class FakeEntryRepository : EntryRepository {
        var updated: Entry? = null

        override fun observeMonth(month: YearMonth): Flow<Result<List<Entry>>> = emptyFlow()

        override suspend fun getEntry(id: String): Result<Entry?> = Result.success(null)

        override suspend fun addEntry(entry: Entry): Result<Unit> = Result.success(Unit)

        override suspend fun updateEntry(entry: Entry): Result<Unit> {
            updated = entry
            return Result.success(Unit)
        }

        override suspend fun deleteEntry(id: String): Result<Unit> = Result.success(Unit)
    }

    private val repository = FakeEntryRepository()
    private val updateEntry = UpdateEntryUseCase(repository)

    private val ride = Entry(
        id = "e1",
        type = EntryType.EXPENSE,
        amount = 780,
        category = "Rides",
        account = "Cash wallet",
        date = LocalDate.of(2026, 10, 10),
        note = "  Ride home after team lunch  ",
        spendingType = SpendingType.DISCRETIONARY
    )

    @Test
    fun validEdit_isSavedWithTheNoteTrimmed() = runBlocking {
        val result = updateEntry(ride)

        assertTrue(result.isSuccess)
        assertEquals("Ride home after team lunch", repository.updated?.note)
        assertEquals("e1", repository.updated?.id)
    }

    @Test
    fun userChoiceOverridesTheCategoryDefault() = runBlocking {
        // Rent is usually committed; the user has marked this one discretionary.
        updateEntry(ride.copy(category = "Rent", spendingType = SpendingType.DISCRETIONARY))

        assertEquals(SpendingType.DISCRETIONARY, repository.updated?.spendingType)
    }

    @Test
    fun zeroAmount_isRejectedAndNotSaved() = runBlocking {
        val result = updateEntry(ride.copy(amount = 0))

        assertEquals("Enter an amount.", result.exceptionOrNull()?.message)
        assertNull(repository.updated)
    }

    @Test
    fun expenseWithoutSpendingType_isRejected() = runBlocking {
        val result = updateEntry(ride.copy(spendingType = null))

        assertEquals("Pick committed or discretionary.", result.exceptionOrNull()?.message)
    }

    @Test
    fun incomeWithSpendingType_isRejected() = runBlocking {
        val salary = ride.copy(type = EntryType.INCOME, category = "Salary", amount = 120_000)

        val result = updateEntry(salary)

        assertTrue(result.isFailure)
        assertNull(repository.updated)
    }
}
