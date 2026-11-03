package lk.moneytrail.app.domain.repository

import kotlinx.coroutines.flow.Flow
import lk.moneytrail.app.domain.model.Account
import java.time.LocalDate

// Works on the signed-in user's accounts. Failures are returned as an
// lk.moneytrail.app.domain.model.AppException whose message can be shown to the user.
interface AccountRepository {

    /** The saved accounts in onboarding order; empty until onboarding is finished. Updates live. */
    fun observeAccounts(): Flow<Result<List<Account>>>

    /** Saves [accounts], replacing any saved before under the same ids. */
    suspend fun saveAccounts(accounts: List<Account>): Result<Unit>

    /** Remembers that the account held [balance] LKR at the start of [date], the start for the next balancing. */
    suspend fun saveBalanceCheck(accountId: String, balance: Long, date: LocalDate): Result<Unit>
}
