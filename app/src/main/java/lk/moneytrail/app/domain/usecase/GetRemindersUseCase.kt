package lk.moneytrail.app.domain.usecase

import kotlinx.coroutines.flow.first
import lk.moneytrail.app.domain.model.Reminder
import lk.moneytrail.app.domain.model.dueExpected
import lk.moneytrail.app.domain.model.isSavingsDue
import lk.moneytrail.app.domain.model.monthlySavings
import lk.moneytrail.app.domain.repository.EntryRepository
import lk.moneytrail.app.domain.repository.PlanRepository
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * What to remind the user about [today], by the same rules as the cards inside the app:
 * money that's due, paying themselves first, and catching up after a break. A part that
 * can't be read is left out rather than stopping the others.
 */
class GetRemindersUseCase @Inject constructor(
    private val entryRepository: EntryRepository,
    private val planRepository: PlanRepository,
    private val checkCatchUp: CheckCatchUpUseCase
) {
    suspend operator fun invoke(today: LocalDate): List<Reminder> = buildList {
        val due = entryRepository.observeExpected().first().getOrNull().orEmpty().dueExpected(today)
        if (due.isNotEmpty()) add(Reminder.MoneyDue(due))

        planRepository.getPlan().getOrNull()?.let { plan ->
            val entries = entryRepository.observeMonth(YearMonth.from(today)).first().getOrNull().orEmpty()
            val isSalaryConfirmed = entries.any { it.isSalary && !it.isExpected }
            val amount = plan.monthlySavings(today).values.sum()
            if (amount > 0 && plan.isSavingsDue(today, isSalaryConfirmed)) {
                add(Reminder.PayYourselfFirst(amount))
            }
        }

        checkCatchUp(today)?.let { add(Reminder.CatchUp(it)) }
    }
}
