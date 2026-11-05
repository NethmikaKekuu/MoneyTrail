package lk.moneytrail.app.presentation.freelance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.moneytrail.app.domain.repository.FreelanceRepository
import java.time.LocalDate
import javax.inject.Inject

/** The projects list. Changes to a project happen on its own screen. */
@HiltViewModel
class FreelanceViewModel @Inject constructor(
    private val freelanceRepository: FreelanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FreelanceUiState(isLoading = true))
    val uiState: StateFlow<FreelanceUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        observeProjects()
    }

    fun onRetry() {
        observeProjects()
    }

    private fun observeProjects() {
        observeJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        observeJob = viewModelScope.launch {
            freelanceRepository.observeProjects().collect { result ->
                result
                    .onSuccess { projects ->
                        _uiState.update { it.copy(isLoading = false, projects = projects, today = LocalDate.now(), errorMessage = null) }
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Couldn't load freelance projects.") }
                    }
            }
        }
    }
}
