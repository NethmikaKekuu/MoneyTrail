package lk.moneytrail.app.domain.usecase

import kotlinx.coroutines.flow.first
import lk.moneytrail.app.domain.model.Account
import lk.moneytrail.app.domain.model.calculatedBalance
import lk.moneytrail.app.domain.model.notLoggedEntry
import lk.moneytrail.app.domain.model.startOfDayBalance
import lk.moneytrail.app.domain.repository.AccountRepository
import lk.moneytrail.app.domain.repository.EntryRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Balancing an account (MoneyTrail-Figma-HTML/21-MonthlyClose.html): the user types what
 * the bank really shows, the app compares it with what it worked out, and anything not
 * logged is added as a "Not logged" entry, so the account matches the bank again. The
 * balance is remembered as the start for next time; the first time, that's all it does.
 * Onboarding can set that first balance too ([lk.moneytrail.app.domain.model.Account.checkedBalance]).
 */
class BalanceAccountUseCase @Inject constructor(
    private val entryRepository: EntryRepository,
    private val accountRepository: AccountRepository
) {
    /** What the app thinks [account] holds today; null before it's ever been balanced. */
    suspend fun calculated(account: Account, today: LocalDate): Result<Long?> {
        val checkedOn = account.checkedOn ?: return Result.success(null)
        // From the check day itself: the check remembers the balance at the start of that day.
        val entries = entryRepository.observeBetween(checkedOn, today).first()
            .getOrElse { return Result.failure(it) }
        return Result.success(account.calculatedBalance(entries))
    }

    /** Balances [account] against [realBalance]. Returns how far off it was: below 0 means money left unlogged. */
    suspend operator fun invoke(account: Account, realBalance: Long, today: LocalDate): Result<Long> {
        val calculated = calculated(account, today).getOrElse { return Result.failure(it) }
        val difference = calculated?.let { realBalance - it } ?: 0L
        // Read before the "Not logged" entry is added, which then joins them below.
        val todaysEntries = entryRepository.observeBetween(today, today).first()
            .getOrElse { return Result.failure(it) }

        val notLogged = notLoggedEntry(account.name, difference, today)
        notLogged?.let { entry ->
            entryRepository.addEntry(entry).onFailure { return Result.failure(it) }
        }
        val startOfDay = startOfDayBalance(realBalance, account.name, todaysEntries + listOfNotNull(notLogged))
        return accountRepository.saveBalanceCheck(account.id, startOfDay, today).map { difference }
    }
}
