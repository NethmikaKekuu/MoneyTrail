package lk.moneytrail.app.domain.usecase

import kotlinx.coroutines.flow.first
import lk.moneytrail.app.domain.model.Account
import lk.moneytrail.app.domain.model.calculatedBalance
import lk.moneytrail.app.domain.model.notLoggedEntry
import lk.moneytrail.app.domain.repository.AccountRepository
import lk.moneytrail.app.domain.repository.EntryRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Balancing an account (MoneyTrail-Figma-HTML/21-MonthlyClose.html): the user types what
 * the bank really shows, the app compares it with what it worked out, and anything not
 * logged is added as a "Not logged" entry, so the account matches the bank again. The
 * real balance becomes the start for next time; the first time, that's all it does.
 */
class BalanceAccountUseCase @Inject constructor(
    private val entryRepository: EntryRepository,
    private val accountRepository: AccountRepository
) {
    /** What the app thinks [account] holds today; null before it's ever been balanced. */
    suspend fun calculated(account: Account, today: LocalDate): Result<Long?> {
        val checkedOn = account.checkedOn ?: return Result.success(null)
        // Entries from the day after the check: the typed balance already held that day's money.
        if (!checkedOn.isBefore(today)) return Result.success(account.checkedBalance)
        val entries = entryRepository.observeBetween(checkedOn.plusDays(1), today).first()
            .getOrElse { return Result.failure(it) }
        return Result.success(account.calculatedBalance(entries))
    }

    /** Balances [account] against [realBalance]. Returns how far off it was: below 0 means money left unlogged. */
    suspend operator fun invoke(account: Account, realBalance: Long, today: LocalDate): Result<Long> {
        val calculated = calculated(account, today).getOrElse { return Result.failure(it) }
        val difference = calculated?.let { realBalance - it } ?: 0L

        notLoggedEntry(account.name, difference, today)?.let { entry ->
            entryRepository.addEntry(entry).onFailure { return Result.failure(it) }
        }
        return accountRepository.saveBalanceCheck(account.id, realBalance, today).map { difference }
    }
}
