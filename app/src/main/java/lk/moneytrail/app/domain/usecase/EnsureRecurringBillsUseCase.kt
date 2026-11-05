package lk.moneytrail.app.domain.usecase

import kotlinx.coroutines.flow.first
import lk.moneytrail.app.domain.model.monthsToCreate
import lk.moneytrail.app.domain.model.toExpectedEntry
import lk.moneytrail.app.domain.repository.EntryRepository
import lk.moneytrail.app.domain.repository.RecurringBillRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Puts each active recurring bill on the calendar as an expected expense on its due day,
 * once per month, so it is checked with "Did you pay it?" like any other expected money
 * and turns up in the catch-up after days away.
 *
 * The bill remembers the last month it was put on the calendar, so a bill that was paid,
 * moved, or removed with "Didn't happen" is not offered again for that month. The month
 * is recorded before the entry is added, so two runs can never add it twice; if adding
 * the entry fails, the record is undone so the next run tries again.
 */
class EnsureRecurringBillsUseCase @Inject constructor(
    private val billRepository: RecurringBillRepository,
    private val entryRepository: EntryRepository
) {
    /** Returns how many entries were added, or the first failure. */
    suspend operator fun invoke(today: LocalDate): Result<Int> {
        val bills = billRepository.observeBills().first().getOrElse { return Result.failure(it) }

        var added = 0
        for (bill in bills) {
            var recorded = bill.lastCreatedMonth
            for (month in bill.monthsToCreate(today)) {
                billRepository.markCreated(bill.id, month).getOrElse { return Result.failure(it) }
                val result = entryRepository.addEntry(bill.toExpectedEntry(month))
                if (result.isFailure) {
                    // Back to the last month that really made it onto the calendar.
                    billRepository.markCreated(bill.id, recorded)
                    return Result.failure(result.exceptionOrNull()!!)
                }
                recorded = month
                added++
            }
        }
        return Result.success(added)
    }
}
