package lk.moneytrail.app.domain.repository

import java.time.LocalDate

/**
 * Remembers on the phone when the user last caught up and when they put it off, so the
 * catch-up is offered after a break but not again and again.
 */
interface CatchUpRepository {

    /** The last day the user finished a catch-up; null before the first. */
    fun lastCatchUpOn(): LocalDate?

    fun setLastCatchUpOn(date: LocalDate)

    /** "Later": the day the user put the catch-up off, so it isn't offered again that day. */
    fun snoozedOn(): LocalDate?

    fun setSnoozedOn(date: LocalDate)
}
