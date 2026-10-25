package lk.moneytrail.app.data

import lk.moneytrail.app.data.local.toEntity
import lk.moneytrail.app.data.local.toEntry
import lk.moneytrail.app.domain.model.Categories
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryStatus
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.SpendingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class EntryEntityTest {

    private val coffee = Entry(
        id = "abc123",
        type = EntryType.EXPENSE,
        amount = 850,
        category = "Coffee",
        account = "Cash wallet",
        date = LocalDate.of(2026, 10, 24),
        note = "Flat white",
        spendingType = SpendingType.DISCRETIONARY
    )

    @Test
    fun expense_comesBackFromRoomUnchanged() {
        val row = coffee.toEntity(uid = "user1", createdAt = 1_000L)

        assertEquals("2026-10-24", row.date)
        assertEquals("user1", row.uid)
        assertEquals(coffee, row.toEntry())
    }

    @Test
    fun expectedSalaryWithBonus_comesBackFromRoomUnchanged() {
        val salary = Entry(
            id = "salary-oct",
            type = EntryType.INCOME,
            amount = 135_000,
            category = Categories.SALARY,
            account = "Main bank",
            date = LocalDate.of(2026, 10, 25),
            note = "",
            spendingType = null,
            status = EntryStatus.EXPECTED,
            bonus = 15_000
        )

        assertEquals(salary, salary.toEntity(uid = "user1", createdAt = 0L).toEntry())
    }

    @Test
    fun rowWithUnknownType_isSkipped() {
        val row = coffee.toEntity(uid = "user1", createdAt = 0L).copy(type = "GIFT")

        assertNull(row.toEntry())
    }

    @Test
    fun rowWithBadDate_isSkipped() {
        val row = coffee.toEntity(uid = "user1", createdAt = 0L).copy(date = "24/10/2026")

        assertNull(row.toEntry())
    }

    @Test
    fun rowWithoutKnownStatus_isConfirmed() {
        val row = coffee.toEntity(uid = "user1", createdAt = 0L).copy(status = "")

        assertEquals(EntryStatus.CONFIRMED, row.toEntry()?.status)
    }
}
