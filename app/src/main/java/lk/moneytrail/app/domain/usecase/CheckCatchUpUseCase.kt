package lk.moneytrail.app.domain.usecase

import lk.moneytrail.app.domain.model.daysAway
import lk.moneytrail.app.domain.model.isCatchUpDue
import lk.moneytrail.app.domain.repository.CatchUpRepository
import lk.moneytrail.app.domain.repository.EntryRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * How many days the user has been away when it's time to offer a catch-up, or null when
 * it isn't: someone who logs every few days, a brand new user, or one who said "Later" today.
 */
class CheckCatchUpUseCase @Inject constructor(
    private val entryRepository: EntryRepository,
    private val catchUpRepository: CatchUpRepository
) {
    suspend operator fun invoke(today: LocalDate): Long? {
        // Nothing to offer if the entries can't be read; the next app start tries again.
        val lastLogged = entryRepository.lastLoggedDate().getOrElse { return null }
        val lastCatchUp = catchUpRepository.lastCatchUpOn()
        if (!isCatchUpDue(today, lastLogged, lastCatchUp, catchUpRepository.snoozedOn())) return null
        return daysAway(today, lastLogged, lastCatchUp)
    }
}
