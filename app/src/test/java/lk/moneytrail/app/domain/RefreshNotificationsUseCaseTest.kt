package lk.moneytrail.app.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import lk.moneytrail.app.domain.model.AppNotification
import lk.moneytrail.app.domain.model.Categories
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryStatus
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.NotificationKind
import lk.moneytrail.app.domain.repository.CatchUpRepository
import lk.moneytrail.app.domain.repository.EntryRepository
import lk.moneytrail.app.domain.repository.NotificationLogRepository
import lk.moneytrail.app.domain.repository.PlanRepository
import lk.moneytrail.app.domain.model.MoneyPlan
import lk.moneytrail.app.domain.usecase.CheckCatchUpUseCase
import lk.moneytrail.app.domain.usecase.GetRemindersUseCase
import lk.moneytrail.app.domain.usecase.RefreshNotificationsUseCase
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RefreshNotificationsUseCaseTest {

    private val today = LocalDate.of(2026, 10, 25)

    private class FakeEntryRepository(private val entries: List<Entry>) : EntryRepository {
        override fun observeBetween(from: LocalDate, to: LocalDate): Flow<Result<List<Entry>>> =
            flowOf(Result.success(entries.filter { !it.date.isBefore(from) && !it.date.isAfter(to) }))

        override fun observeExpected(): Flow<Result<List<Entry>>> = flowOf(Result.success(entries.filter { it.isExpected }))
        override suspend fun getEntry(id: String): Result<Entry?> = Result.success(null)
        override suspend fun addEntry(entry: Entry): Result<Unit> = Result.success(Unit)
        override suspend fun updateEntry(entry: Entry): Result<Unit> = Result.success(Unit)
        override suspend fun deleteEntry(id: String): Result<Unit> = Result.success(Unit)
        override suspend fun lastLoggedDate(): Result<LocalDate?> = Result.success(LocalDate.of(2026, 10, 25))
    }

    private class FakePlanRepository : PlanRepository {
        override suspend fun savePlan(plan: MoneyPlan): Result<Unit> = Result.success(Unit)
        override suspend fun getPlan(): Result<MoneyPlan?> = Result.success(null)
        override fun observePlan(): Flow<Result<MoneyPlan?>> = flowOf(Result.success(null))
    }

    private class FakeCatchUpRepository : CatchUpRepository {
        override fun lastCatchUpOn(): LocalDate? = null
        override fun setLastCatchUpOn(date: LocalDate) = Unit
        override fun snoozedOn(): LocalDate? = null
        override fun setSnoozedOn(date: LocalDate) = Unit
    }

    private class FakeNotificationLog : NotificationLogRepository {
        val markedRead = mutableListOf<NotificationKind>()
        override fun observeUnread(): Flow<List<AppNotification>> = flowOf(emptyList())
        override suspend fun add(kind: NotificationKind, title: String, text: String, day: LocalDate) = Unit
        override suspend fun markKindRead(kind: NotificationKind) {
            markedRead += kind
        }
    }

    private fun refresh(entries: List<Entry>): List<NotificationKind> = runBlocking {
        val entryRepository = FakeEntryRepository(entries)
        val log = FakeNotificationLog()
        val getReminders = GetRemindersUseCase(entryRepository, FakePlanRepository(), CheckCatchUpUseCase(entryRepository, FakeCatchUpRepository()))
        RefreshNotificationsUseCase(getReminders, log)(today)
        log.markedRead
    }

    private fun salary(status: EntryStatus) =
        Entry("s1", EntryType.INCOME, 132_400, Categories.SALARY, "Main bank", today, "", null, status)

    @Test
    fun aReminderStillPending_staysUnread() {
        val read = refresh(listOf(salary(EntryStatus.EXPECTED)))

        // Money is still due, so only the other two kinds are cleared.
        assertEquals(setOf(NotificationKind.PAY_YOURSELF_FIRST, NotificationKind.CATCH_UP), read.toSet())
    }

    @Test
    fun aReminderWhoseActionIsDone_isMarkedRead() {
        val read = refresh(listOf(salary(EntryStatus.CONFIRMED)))

        assertEquals(NotificationKind.entries.toSet(), read.toSet())
    }
}
