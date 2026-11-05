package lk.moneytrail.app.presentation.bills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.moneytrail.app.domain.model.Accounts
import lk.moneytrail.app.domain.model.Categories
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.RecurringBill
import lk.moneytrail.app.domain.repository.AccountRepository
import lk.moneytrail.app.domain.repository.CategoryRepository
import lk.moneytrail.app.domain.repository.RecurringBillRepository
import lk.moneytrail.app.domain.usecase.SaveRecurringBillUseCase
import java.time.LocalDate
import javax.inject.Inject

/**
 * The list of recurring bills, and adding, editing, stopping and deleting them. The rules
 * live in [SaveRecurringBillUseCase]; this turns taps into calls and results into state.
 */
@HiltViewModel
class RecurringBillsViewModel @Inject constructor(
    private val billRepository: RecurringBillRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val saveBill: SaveRecurringBillUseCase
) : ViewModel() {

    // The defaults and built-in categories show at once; the user's own replace them when they arrive.
    private val _uiState = MutableStateFlow(
        RecurringBillsState(
            accountNames = Accounts.defaults,
            categoryNames = Categories.namesFor(EntryType.EXPENSE)
        )
    )
    val uiState: StateFlow<RecurringBillsState> = _uiState.asStateFlow()

    private var billsJob: Job? = null

    init {
        observeBills()
        observeAccounts()
        observeCategories()
    }

    fun onRetryClick() {
        observeBills()
    }

    private fun observeBills() {
        billsJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        billsJob = viewModelScope.launch {
            billRepository.observeBills().collect { result ->
                result
                    .onSuccess { bills -> _uiState.update { it.copy(isLoading = false, loadError = null, bills = bills) } }
                    .onFailure { e ->
                        _uiState.update { it.copy(isLoading = false, loadError = e.message ?: "Couldn't load your recurring bills.") }
                    }
            }
        }
    }

    private fun observeAccounts() {
        viewModelScope.launch {
            accountRepository.observeAccounts().collect { result ->
                // Only the account choices depend on this, so a failure keeps the defaults.
                result.onSuccess { accounts ->
                    val names = accounts.map { it.name }.ifEmpty { Accounts.defaults }
                    _uiState.update { it.copy(accountNames = names) }
                }
            }
        }
    }

    private fun observeCategories() {
        viewModelScope.launch {
            categoryRepository.observeCategories().collect { result ->
                // Likewise the user's own categories are a bonus; the built-in ones always show.
                val custom = result.getOrNull().orEmpty()
                _uiState.update { it.copy(categoryNames = Categories.namesFor(EntryType.EXPENSE, custom)) }
            }
        }
    }

    fun onAddClick() {
        val state = _uiState.value
        _uiState.update { it.copy(draft = BillDraft(account = state.accountNames.firstOrNull().orEmpty())) }
    }

    fun onEditClick(bill: RecurringBill) {
        _uiState.update { it.copy(draft = BillDraft.of(bill)) }
    }

    fun onDraftDismiss() {
        _uiState.update { if (it.isSaving) it else it.copy(draft = null) }
    }

    fun onNameChange(text: String) = updateDraft { copy(name = text.take(MAX_NAME)) }

    fun onAmountChange(text: String) = updateDraft { copy(amountDigits = text.filter(Char::isDigit).trimStart('0').take(MAX_DIGITS)) }

    fun onAccountSelect(name: String) = updateDraft { copy(account = name) }

    fun onCategorySelect(name: String) = updateDraft { copy(category = name) }

    fun onDueDayChange(text: String) = updateDraft { copy(dueDayDigits = text.filter(Char::isDigit).take(2)) }

    fun onSaveClick() {
        val state = _uiState.value
        val draft = state.draft ?: return
        if (state.isSaving) return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            saveBill(draft.toBill(), LocalDate.now())
                .onSuccess { _uiState.update { it.copy(isSaving = false, draft = null) } }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isSaving = false, draft = it.draft?.copy(errorMessage = e.message ?: "Couldn't save the bill."))
                    }
                }
        }
    }

    /** Stops or restarts a bill. A stopped bill makes no new entries and keeps its history. */
    fun onActiveChange(bill: RecurringBill, isActive: Boolean) {
        viewModelScope.launch {
            // Saved as it is, so its record of the last month it was put on the calendar is kept.
            billRepository.saveBill(bill.copy(isActive = isActive))
                .onFailure { e -> _uiState.update { it.copy(message = e.message ?: "Couldn't update the bill.") } }
        }
    }

    fun onDeleteClick(bill: RecurringBill) {
        _uiState.update { it.copy(deleting = bill) }
    }

    fun onDeleteDismiss() {
        _uiState.update { it.copy(deleting = null) }
    }

    fun onDeleteConfirm() {
        val bill = _uiState.value.deleting ?: return
        _uiState.update { it.copy(deleting = null) }
        viewModelScope.launch {
            billRepository.deleteBill(bill.id)
                .onFailure { e -> _uiState.update { it.copy(message = e.message ?: "Couldn't delete the bill.") } }
        }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    private fun updateDraft(change: BillDraft.() -> BillDraft) {
        _uiState.update { state -> state.draft?.let { state.copy(draft = it.change().copy(errorMessage = null)) } ?: state }
    }

    private companion object {
        const val MAX_NAME = 60

        /** LKR 999,999,999 at most, as everywhere else in the app. */
        const val MAX_DIGITS = 9
    }
}
