package lk.moneytrail.app.domain.usecase

import lk.moneytrail.app.domain.model.AppException
import lk.moneytrail.app.domain.model.RecurringBill
import lk.moneytrail.app.domain.model.RecurringBills
import lk.moneytrail.app.domain.model.dueDateIn
import lk.moneytrail.app.domain.repository.RecurringBillRepository
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Saves a new or edited recurring bill after checking it.
 *
 * A bill added after this month's due day has passed starts next month, so the user isn't
 * asked about a payment they have probably already made.
 */
class SaveRecurringBillUseCase @Inject constructor(
    private val billRepository: RecurringBillRepository
) {
    suspend operator fun invoke(bill: RecurringBill, today: LocalDate): Result<Unit> {
        val cleaned = bill.copy(name = bill.name.trim())
        RecurringBills.billError(cleaned)?.let { return Result.failure(AppException(it)) }

        val thisMonth = YearMonth.from(today)
        val skipThisMonth = cleaned.id.isEmpty() && cleaned.dueDateIn(thisMonth).isBefore(today)
        return billRepository.saveBill(
            if (skipThisMonth) cleaned.copy(lastCreatedMonth = thisMonth) else cleaned
        )
    }
}
