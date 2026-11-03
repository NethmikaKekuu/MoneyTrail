package lk.moneytrail.app.domain.usecase

import kotlinx.coroutines.flow.first
import lk.moneytrail.app.domain.model.Account
import lk.moneytrail.app.domain.model.MonthClose
import lk.moneytrail.app.domain.model.balanceAtEndOf
import lk.moneytrail.app.domain.model.notLoggedEntry
import lk.moneytrail.app.domain.repository.AccountRepository
import lk.moneytrail.app.domain.repository.EntryRepository
import lk.moneytrail.app.domain.repository.MonthCloseRepository
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Closing a month (MoneyTrail-Figma-HTML/21-MonthlyClose.html): the user types each
 * account's real balance on the last day. Anything the app didn't know about is added as
 * a "Not logged" entry on that day, so the month's numbers match the bank; the balances
 * are saved as the close and become each account's start for the next month.
 */
class CloseMonthUseCase @Inject constructor(
    private val entryRepository: EntryRepository,
    private val accountRepository: AccountRepository,
    private val monthCloseRepository: MonthCloseRepository
) {
    /** What the app worked out for each account at the end of [month], by account id; null where it can't. */
    suspend fun calculated(month: YearMonth, accounts: List<Account>): Result<Map<String, Long?>> {
        val end = month.atEndOfMonth()
        val from = accounts.mapNotNull { it.checkedOn }.minOrNull() ?: return Result.success(accounts.associate { it.id to null })
        if (from.isAfter(end.plusDays(1))) return Result.success(accounts.associate { it.id to null })
        val entries = entryRepository.observeBetween(from, end).first().getOrElse { return Result.failure(it) }
        return Result.success(accounts.associate { it.id to it.balanceAtEndOf(end, entries) })
    }

    /** Closes [month] with [realBalances] (by account id) for [accounts]. */
    suspend operator fun invoke(
        month: YearMonth,
        accounts: List<Account>,
        realBalances: Map<String, Long>,
        today: LocalDate
    ): Result<Unit> {
        if (realBalances.isEmpty()) return Result.failure(IllegalArgumentException("Type at least one real balance."))
        val end = month.atEndOfMonth()
        val calculated = calculated(month, accounts).getOrElse { return Result.failure(it) }

        accounts.filter { it.id in realBalances }.forEach { account ->
            val real = realBalances.getValue(account.id)
            calculated[account.id]?.let { worked ->
                notLoggedEntry(account.name, real - worked, end)?.let { entry ->
                    entryRepository.addEntry(entry).onFailure { return Result.failure(it) }
                }
            }
            // The real balance is what it held at the start of next month: the start for
            // balancing from now on, unless the account was already checked later.
            val nextStart = end.plusDays(1)
            if (account.checkedOn == null || !account.checkedOn.isAfter(nextStart)) {
                accountRepository.saveBalanceCheck(account.id, real, nextStart).onFailure { return Result.failure(it) }
            }
        }
        return monthCloseRepository.saveClose(MonthClose(month, realBalances, today))
    }
}
