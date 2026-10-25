package lk.moneytrail.app.domain.model

import java.time.LocalDate

/**
 * Expected entries whose date has come: today's, and any older ones still not checked,
 * oldest first. A forgotten invoice keeps showing until the user deals with it.
 */
fun List<Entry>.dueExpected(today: LocalDate): List<Entry> =
    filter { it.isExpected && !it.date.isAfter(today) }
        .sortedWith(compareBy<Entry> { it.date }.thenBy { it.category })

/** The entry once the money has really moved: [amount] is what arrived or was paid, on [date]. */
fun Entry.confirmed(amount: Long, date: LocalDate): Entry =
    copy(status = EntryStatus.CONFIRMED, amount = amount, date = date)

/** A salary once it has arrived: the [base] pay plus this month's [bonus] make up its amount. */
fun Entry.confirmedSalary(base: Long, bonus: Long, date: LocalDate): Entry =
    confirmed(base + bonus, date).copy(bonus = bonus)

/**
 * A salary confirmed from the [total] that really arrived. Anything above the usual
 * [base] pay is the bonus. If less than the base arrived, there's no bonus and the
 * whole total is the pay, so the entry always matches the bank.
 */
fun Entry.confirmedSalaryFromTotal(total: Long, base: Long, date: LocalDate): Entry {
    val bonus = (total - base).coerceAtLeast(0)
    return confirmedSalary(base = total - bonus, bonus = bonus, date = date)
}
