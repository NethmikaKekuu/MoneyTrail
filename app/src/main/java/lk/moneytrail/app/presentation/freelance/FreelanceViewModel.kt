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
import lk.moneytrail.app.domain.model.Milestone
import lk.moneytrail.app.domain.model.MilestoneStatus
import lk.moneytrail.app.domain.model.Project
import lk.moneytrail.app.domain.repository.FreelanceRepository
import javax.inject.Inject

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

    fun onSelectProject(projectId: String) {
        _uiState.update { it.copy(selectedProjectId = projectId) }
    }

    private fun observeProjects() {
        observeJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        observeJob = viewModelScope.launch {
            freelanceRepository.observeProjects().collect { result ->
                result
                    .onSuccess { projects ->
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                projects = projects,
                                selectedProjectId = state.selectedProjectId ?: projects.firstOrNull()?.id
                            )
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Couldn't load freelance projects."
                            )
                        }
                    }
            }
        }
    }

    /** Marks the first PLANNED milestone of selected project as INVOICED */
    fun onMarkInvoiced() {
        val selected = _uiState.value.selectedProject ?: return
        val milestoneToInvoice = selected.milestones.find { it.status == MilestoneStatus.PLANNED } ?: return
        val updatedMilestones = selected.milestones.map {
            if (it.id == milestoneToInvoice.id) it.copy(status = MilestoneStatus.INVOICED) else it
        }
        val updatedProject = selected.copy(milestones = updatedMilestones)
        saveProjectUpdate(updatedProject)
    }


    private fun saveProjectUpdate(project: Project) {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            freelanceRepository.saveProject(project)
                .onSuccess {
                    _uiState.update { state ->
                        val updatedList = state.projects.map { if (it.id == project.id) project else it }
                        state.copy(isSaving = false, projects = updatedList)
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = error.message) }
                }
        }
    }

}
