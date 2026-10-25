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
