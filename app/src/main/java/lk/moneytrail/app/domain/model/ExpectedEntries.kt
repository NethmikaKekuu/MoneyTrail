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
