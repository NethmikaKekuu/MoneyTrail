package lk.moneytrail.app.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.SpendingType
import lk.moneytrail.app.domain.repository.EntryRepository
import lk.moneytrail.app.domain.usecase.AddEntryUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AddEntryUseCaseTest {

    /** Keeps what would have been saved, instead of talking to Firestore. */
    private class FakeEntryRepository : EntryRepository {
        val saved = mutableListOf<Entry>()

        override fun observeBetween(from: LocalDate, to: LocalDate): Flow<Result<List<Entry>>> = emptyFlow()

        override fun observeExpected(): Flow<Result<List<Entry>>> = emptyFlow()

        override suspend fun getEntry(id: String): Result<Entry?> = Result.success(null)

        override suspend fun addEntry(entry: Entry): Result<Unit> {
            saved += entry
            return Result.success(Unit)
        }

        override suspend fun updateEntry(entry: Entry): Result<Unit> = Result.success(Unit)

        override suspend fun deleteEntry(id: String): Result<Unit> = Result.success(Unit)
    }

    private val repository = FakeEntryRepository()
    private val addEntry = AddEntryUseCase(repository)
    private val day = LocalDate.of(2026, 10, 10)

    @Test
    fun expense_getsSpendingTypeFromItsCategory() = runBlocking {
        addEntry(EntryType.EXPENSE, 34_000, "Rent", "Main bank", day, "")
        addEntry(EntryType.EXPENSE, 850, "Coffee", "Cash wallet", day, "")

        assertEquals(SpendingType.COMMITTED, repository.saved[0].spendingType)
        assertEquals(SpendingType.DISCRETIONARY, repository.saved[1].spendingType)
    }

    @Test
    fun income_hasNoSpendingType() = runBlocking {
        addEntry(EntryType.INCOME, 120_000, "Salary", "Main bank", day, "")

        assertNull(repository.saved.single().spendingType)
    }

    @Test
    fun note_isTrimmed() = runBlocking {
        addEntry(EntryType.EXPENSE, 2_400, "Food out", "Main bank", day, "  Farewell lunch  ")

        assertEquals("Farewell lunch", repository.saved.single().note)
    }

    @Test
    fun zeroAmount_isRejectedAndNotSaved() = runBlocking {
        val result = addEntry(EntryType.EXPENSE, 0, "Coffee", "Cash wallet", day, "")

        assertTrue(result.isFailure)
        assertEquals("Enter an amount.", result.exceptionOrNull()?.message)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun categoryOfTheOtherType_isRejected() = runBlocking {
        val result = addEntry(EntryType.INCOME, 5_000, "Coffee", "Main bank", day, "")

        assertTrue(result.isFailure)
        assertTrue(repository.saved.isEmpty())
    }
}
