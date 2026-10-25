package lk.moneytrail.app.presentation.entry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.moneytrail.app.domain.model.Categories
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.SpendingType
import lk.moneytrail.app.domain.repository.AccountRepository
import lk.moneytrail.app.domain.repository.CategoryRepository
import lk.moneytrail.app.domain.repository.EntryRepository
import lk.moneytrail.app.domain.usecase.AddEntryUseCase
import lk.moneytrail.app.domain.usecase.UpdateEntryUseCase
import lk.moneytrail.app.presentation.navigation.Routes
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class EditEntryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val entryRepository: EntryRepository,
    private val updateEntryUseCase: UpdateEntryUseCase,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    private val entryId: String = savedStateHandle.get<String>(Routes.EDIT_ENTRY_ID_ARG).orEmpty()

    private val _uiState = MutableStateFlow(EditEntryState())
    val uiState: StateFlow<EditEntryState> = _uiState.asStateFlow()

    init {
        loadEntry()
        // Offer the accounts picked during onboarding, as on the new entry screen.
        viewModelScope.launch {
            accountRepository.observeAccounts().collect { result ->
                val names = result.getOrNull()?.map { it.name }.orEmpty()
                if (names.isNotEmpty()) _uiState.update { it.copy(accounts = names) }
            }
        }
        viewModelScope.launch {
            categoryRepository.observeCategories().collect { result ->
                result.onSuccess { categories -> _uiState.update { it.copy(customCategories = categories) } }
            }
        }
    }

    fun loadEntry() {
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            entryRepository.getEntry(entryId)
                .onSuccess { entry ->
                    _uiState.update {
                        if (entry == null) {
                            it.copy(isLoading = false, loadError = "This entry has been deleted.")
                        } else {
                            it.copy(
                                isLoading = false,
                                original = entry,
                                draft = entry,
                                amountDigits = entry.baseAmount.toString(),
                                bonusDigits = entry.bonus.takeIf { bonus -> bonus > 0 }?.toString().orEmpty()
                            )
                        }
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, loadError = e.message ?: "Couldn't load this entry.") }
                }
        }
    }

    fun onAmountChange(text: String) {
        val digits = text.filter(Char::isDigit).trimStart('0').take(MAX_DIGITS)
        _uiState.update { it.copy(amountDigits = digits, errorMessage = null) }
    }

    fun onBonusChange(text: String) {
        val digits = text.filter(Char::isDigit).trimStart('0').take(MAX_DIGITS)
        _uiState.update { it.copy(bonusDigits = digits, errorMessage = null) }
    }

    /**
     * A new expense category brings its usual committed or discretionary setting with it,
     * whether it's built in or the user's own. One with no setting keeps the current choice.
     */
    fun onCategoryChange(category: String) {
        _uiState.update { state ->
            val draft = state.draft ?: return@update state
            val spendingType = if (draft.type == EntryType.EXPENSE) {
                Categories.defaultSpending(category, state.customCategories) ?: draft.spendingType
            } else {
                null
            }
            val changed = draft.copy(category = category, spendingType = spendingType)
            // Only a salary has a bonus, so moving away from Salary keeps it in the amount.
            if (state.isSalary && !changed.isSalary) {
                val whole = state.amount.takeIf { it > 0 }?.toString().orEmpty()
                state.copy(draft = changed, amountDigits = whole, bonusDigits = "", errorMessage = null)
            } else {
                state.copy(draft = changed, errorMessage = null)
            }
        }
    }

    fun onAccountChange(account: String) {
        updateDraft { it.copy(account = account) }
    }

    fun onSpendingTypeChange(spendingType: SpendingType) {
        updateDraft { if (it.type == EntryType.EXPENSE) it.copy(spendingType = spendingType) else it }
    }

    fun onDateChange(date: LocalDate) {
        updateDraft { it.copy(date = date) }
    }

    fun onNoteChange(note: String) {
        updateDraft { it.copy(note = note.take(AddEntryUseCase.MAX_NOTE_LENGTH)) }
    }

    fun save() {
        val state = _uiState.value
        val draft = state.draft ?: return
        if (state.isSaving) return

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            updateEntryUseCase(draft.copy(amount = state.amount, bonus = state.bonus))
                .onSuccess { _uiState.update { it.copy(isSaving = false, isDone = true) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, errorMessage = e.message) } }
        }
    }

    fun onDeleteClick() {
        _uiState.update { it.copy(isConfirmingDelete = true, errorMessage = null) }
    }

    fun onDeleteDismiss() {
        _uiState.update { it.copy(isConfirmingDelete = false) }
    }

    fun onDeleteConfirm() {
        if (_uiState.value.isSaving) return

        _uiState.update { it.copy(isConfirmingDelete = false, isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            entryRepository.deleteEntry(entryId)
                .onSuccess { _uiState.update { it.copy(isSaving = false, isDone = true) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, errorMessage = e.message) } }
        }
    }

    private fun updateDraft(change: (Entry) -> Entry) {
        _uiState.update { state ->
            val draft = state.draft ?: return@update state
            state.copy(draft = change(draft), errorMessage = null)
        }
    }

    private companion object {
        /** LKR 999,999,999 at most, as on the new entry keypad. */
        const val MAX_DIGITS = 9
    }
}
