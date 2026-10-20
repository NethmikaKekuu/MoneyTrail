package lk.moneytrail.app.presentation.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.moneytrail.app.domain.repository.EntryRepository
import lk.moneytrail.app.domain.repository.PlanRepository
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val entryRepository: EntryRepository,
    private val planRepository: PlanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsState(today = LocalDate.now()))
    val uiState: StateFlow<InsightsState> = _uiState.asStateFlow()

    private var listenJob: Job? = null

    init {
        listenToEntries()
        loadGoal()
    }

    fun onTabClick(tab: InsightsTab) {
        _uiState.update { it.copy(tab = tab) }
    }

    fun onRetryClick() {
        listenToEntries()
        loadGoal()
    }

    /** One live listener for every month in the chart, so the insights follow the calendar. */
    private fun listenToEntries() {
        listenJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        listenJob = viewModelScope.launch {
            val month = _uiState.value.month
            val from = month.minusMonths((InsightsState.CHART_MONTHS - 1).toLong()).atDay(1)
            entryRepository.observeBetween(from, month.atEndOfMonth()).collect { result ->
                result
                    .onSuccess { entries ->
                        _uiState.update { it.copy(isLoading = false, errorMessage = null, entries = entries) }
                    }
                    .onFailure { e ->
                        _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Couldn't load your insights.") }
                    }
            }
        }
    }

    private fun loadGoal() {
        viewModelScope.launch {
            // Only the goal note depends on this, so a failure just leaves it out.
            planRepository.getPlan().onSuccess { plan ->
                _uiState.update { it.copy(goal = plan?.goal) }
            }
        }
    }
}
