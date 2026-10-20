package lk.moneytrail.app.presentation.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.repository.EntryRepository
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class FoldersViewModel @Inject constructor(
    private val entryRepository: EntryRepository
) : ViewModel() {

    private val currentMonth = YearMonth.now()

    private val _uiState = MutableStateFlow(FoldersState(currentMonth = currentMonth))
    val uiState: StateFlow<FoldersState> = _uiState.asStateFlow()

    private var listenJob: Job? = null

    init {
        listenToEntries()
    }

    fun onFolderClick(type: EntryType) {
        _uiState.update { it.copy(selectedType = type) }
    }

    fun onMonthClick(month: YearMonth) {
        _uiState.update { it.copy(selectedMonth = month) }
    }

    fun onRetryClick() {
        listenToEntries()
    }

    /**
     * One live listener for the current month and the [MONTHS_BACK] before it, so the
     * folders update as entries are added, edited or deleted on the calendar.
     */
    private fun listenToEntries() {
        listenJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        listenJob = viewModelScope.launch {
            val from = currentMonth.minusMonths(MONTHS_BACK).atDay(1)
            entryRepository.observeBetween(from, currentMonth.atEndOfMonth()).collect { result ->
                result
                    .onSuccess { entries ->
                        _uiState.update { it.copy(isLoading = false, errorMessage = null, entries = entries) }
                    }
                    .onFailure { e ->
                        _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Couldn't load your folders.") }
                    }
            }
        }
    }

    private companion object {
        /** How many earlier months the folders reach back. */
        const val MONTHS_BACK = 6L
    }
}
