package lk.moneytrail.app.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.moneytrail.app.domain.repository.AuthRepository
import lk.moneytrail.app.domain.repository.EntryRepository
import lk.moneytrail.app.domain.repository.UserRepository
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val entryRepository: EntryRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val today = LocalDate.now()

    private val _uiState = MutableStateFlow(
        CalendarState(month = YearMonth.from(today), today = today, selectedDate = today)
    )
    val uiState: StateFlow<CalendarState> = _uiState.asStateFlow()

    /** The live listener for the month on screen. Replaced when the month changes. */
    private var monthJob: Job? = null

    init {
        loadFirstName()
        listenToMonth(_uiState.value.month)
    }

    fun onPreviousMonthClick() {
        showMonth(_uiState.value.month.minusMonths(1))
    }

    fun onNextMonthClick() {
        showMonth(_uiState.value.month.plusMonths(1))
    }

    fun onDayClick(date: LocalDate) {
        if (YearMonth.from(date) == _uiState.value.month) {
            _uiState.update { it.copy(selectedDate = date) }
        }
    }

    fun onRetryClick() {
        listenToMonth(_uiState.value.month)
    }

    private fun showMonth(month: YearMonth) {
        // Land on today when it's in that month, otherwise on the 1st.
        val selected = if (YearMonth.from(today) == month) today else month.atDay(1)
        _uiState.update { it.copy(month = month, selectedDate = selected, entries = emptyList()) }
        listenToMonth(month)
    }

    private fun listenToMonth(month: YearMonth) {
        monthJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        monthJob = viewModelScope.launch {
            entryRepository.observeMonth(month).collect { result ->
                result
                    .onSuccess { entries ->
                        _uiState.update { it.copy(isLoading = false, errorMessage = null, entries = entries) }
                    }
                    .onFailure { e ->
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = e.message ?: "Couldn't load your entries.")
                        }
                    }
            }
        }
    }

    private fun loadFirstName() {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            // Only the greeting depends on this, so a failure just leaves it as "Hi there".
            userRepository.getProfile(uid).onSuccess { profile ->
                val firstName = profile?.name?.trim()?.substringBefore(' ')?.takeIf { it.isNotEmpty() }
                _uiState.update { it.copy(firstName = firstName) }
            }
        }
    }
}
