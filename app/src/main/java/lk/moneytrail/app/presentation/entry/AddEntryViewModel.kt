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
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.usecase.AddEntryUseCase
import lk.moneytrail.app.presentation.navigation.Routes
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AddEntryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val addEntryUseCase: AddEntryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEntryState(date = startDate(savedStateHandle)))
    val uiState: StateFlow<AddEntryState> = _uiState.asStateFlow()

    fun onTypeChange(type: EntryType) {
        _uiState.update {
            if (it.type == type) it
            else it.copy(type = type, category = Categories.namesFor(type).first(), errorMessage = null)
        }
    }

    fun onKeyPress(key: String) {
        _uiState.update { it.copy(amountDigits = AmountInput.type(it.amountDigits, key), errorMessage = null) }
    }

    fun onDeletePress() {
        _uiState.update { it.copy(amountDigits = AmountInput.delete(it.amountDigits), errorMessage = null) }
    }

    fun onCategoryChange(category: String) {
        _uiState.update { it.copy(category = category, errorMessage = null) }
    }

    fun onAccountChange(account: String) {
        _uiState.update { it.copy(account = account, errorMessage = null) }
    }

    fun onDateChange(date: LocalDate) {
        _uiState.update { it.copy(date = date, errorMessage = null) }
    }

    fun onNoteChange(note: String) {
        _uiState.update { it.copy(note = note.take(AddEntryUseCase.MAX_NOTE_LENGTH), errorMessage = null) }
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            addEntryUseCase(
                type = state.type,
                amount = state.amount,
                category = state.category,
                account = state.account,
                date = state.date,
                note = state.note
            )
                .onSuccess { _uiState.update { it.copy(isSaving = false, isSaved = true) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, errorMessage = e.message) } }
        }
    }

    private companion object {
        /** The day picked on the calendar, or today if none was passed. */
        fun startDate(savedStateHandle: SavedStateHandle): LocalDate =
            savedStateHandle.get<String>(Routes.ADD_ENTRY_DATE_ARG)
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?: LocalDate.now()
    }
}
