package lk.moneytrail.app.presentation.catchup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.moneytrail.app.domain.model.confirmed
import lk.moneytrail.app.domain.model.daysAway
import lk.moneytrail.app.domain.model.dueExpected
import lk.moneytrail.app.domain.repository.AccountRepository
import lk.moneytrail.app.domain.repository.CatchUpRepository
import lk.moneytrail.app.domain.repository.EntryRepository
import lk.moneytrail.app.domain.usecase.BalanceAccountUseCase
import lk.moneytrail.app.domain.usecase.EnsureRecurringBillsUseCase
import lk.moneytrail.app.domain.usecase.UpdateEntryUseCase
import java.time.LocalDate
import javax.inject.Inject

/**
 * Catch-up after a break: confirm what fell due, log what the user remembers, and balance
 * each account so anything not logged is still counted. Every step can be skipped.
 */
@HiltViewModel
class CatchUpViewModel @Inject constructor(
    private val entryRepository: EntryRepository,
    private val accountRepository: AccountRepository,
    private val catchUpRepository: CatchUpRepository,
    private val updateEntry: UpdateEntryUseCase,
    private val balanceAccount: BalanceAccountUseCase,
    private val ensureRecurringBills: EnsureRecurringBillsUseCase
) : ViewModel() {

    private val today = LocalDate.now()

    private val _uiState = MutableStateFlow(CatchUpState())
    val uiState: StateFlow<CatchUpState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            // Bills that fell due while the user was away are on the list to tick off.
            // If this fails, the bills already on the calendar still show.
            ensureRecurringBills(today)
            val lastLogged = entryRepository.lastLoggedDate().getOrNull()
            val due = entryRepository.observeExpected().first().getOrNull().orEmpty().dueExpected(today)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    daysAway = daysAway(today, lastLogged, catchUpRepository.lastCatchUpOn()),
                    dueEntries = due,
                    // Nothing came due: start with what the user remembers.
                    step = if (due.isEmpty()) CatchUpStep.LOG else CatchUpStep.CONFIRM
                )
            }
            loadBalances()
        }
    }

    /** Each account with what the app thinks it holds, for the balance step. */
    private suspend fun loadBalances() {
        val accounts = accountRepository.observeAccounts().first().getOrNull().orEmpty()
        val rows = accounts.map { account ->
            BalanceRow(account = account, calculated = balanceAccount.calculated(account, today).getOrNull())
        }
        _uiState.update { it.copy(balanceRows = rows) }
    }

    fun onEntryToggle(id: String) {
        _uiState.update {
            it.copy(untickedIds = if (id in it.untickedIds) it.untickedIds - id else it.untickedIds + id)
        }
    }

    /** "Confirm N items": the ticked ones happened as planned, on their own dates. */
    fun onConfirmEntriesClick() {
        val state = _uiState.value
        if (state.isSaving) return
        val ticked = state.dueEntries.filter { it.id !in state.untickedIds }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            ticked.forEach { entry ->
                updateEntry(entry.confirmed(entry.amount, entry.date)).onFailure { e ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = e.message) }
                    return@launch
                }
            }
            _uiState.update { it.copy(isSaving = false, step = CatchUpStep.LOG) }
        }
    }

    fun onSkipConfirmClick() {
        _uiState.update { it.copy(step = CatchUpStep.LOG, errorMessage = null) }
    }

    fun onLogNextClick() {
        viewModelScope.launch {
            // Entries added in the meantime change what the accounts should hold.
            loadBalances()
            _uiState.update { it.copy(step = CatchUpStep.BALANCE, errorMessage = null) }
        }
    }

    fun onRealBalanceChange(accountId: String, text: String) {
        val digits = text.filter(Char::isDigit).trimStart('0').take(MAX_DIGITS)
        _uiState.update { state ->
            state.copy(
                balanceRows = state.balanceRows.map { if (it.account.id == accountId) it.copy(realDigits = digits) else it },
                errorMessage = null
            )
        }
    }

    /** Balances every account the user typed a balance for, then shows what was found. */
    fun onBalanceClick() {
        val state = _uiState.value
        if (state.isSaving) return
        val rows = state.balanceRows.filter { it.real != null }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            var spent = 0L
            var received = 0L
            rows.forEach { row ->
                val difference = balanceAccount(row.account, row.real ?: 0L, today).getOrElse { e ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = e.message) }
                    return@launch
                }
                if (difference < 0) spent -= difference else received += difference
            }
            finishCatchUp()
            _uiState.update {
                it.copy(
                    isSaving = false,
                    step = CatchUpStep.DONE,
                    notLoggedSpent = spent,
                    notLoggedReceived = received,
                    balancedCount = rows.size
                )
            }
        }
    }

    fun onSkipBalanceClick() {
        finishCatchUp()
        _uiState.update { it.copy(step = CatchUpStep.DONE, errorMessage = null) }
    }

    /** "Later": not offered again today. */
    fun onLaterClick() {
        catchUpRepository.setSnoozedOn(today)
        _uiState.update { it.copy(isFinished = true) }
    }

    fun onDoneClick() {
        _uiState.update { it.copy(isFinished = true) }
    }

    /** The user is current again, so the next offer waits for another break. */
    private fun finishCatchUp() {
        catchUpRepository.setLastCatchUpOn(today)
    }

    private companion object {
        /** LKR 999,999,999 at most, as everywhere else in the app. */
        const val MAX_DIGITS = 9
    }
}
