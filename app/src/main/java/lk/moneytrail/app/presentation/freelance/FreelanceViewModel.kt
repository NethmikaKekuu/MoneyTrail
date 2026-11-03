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
                        val initialProjects = projects.ifEmpty { defaultMockProjects() }
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                projects = initialProjects,
                                selectedProjectId = state.selectedProjectId ?: initialProjects.firstOrNull()?.id
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

    /** Marks the first INVOICED milestone of selected project as PAID */
    fun onRecordPayment() {
        val selected = _uiState.value.selectedProject ?: return
        val milestoneToPay = selected.milestones.find { it.status == MilestoneStatus.INVOICED }
            ?: selected.milestones.find { it.status == MilestoneStatus.PLANNED }
            ?: return

        val updatedMilestones = selected.milestones.map {
            if (it.id == milestoneToPay.id) it.copy(status = MilestoneStatus.PAID) else it
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

    companion object {
        /** Default initial projects corresponding to the user's mockup design */
        fun defaultMockProjects(): List<Project> {
            val now = System.currentTimeMillis()
            val dayMs = 86_400_000L

            val bakeryMilestones = listOf(
                Milestone("m1", "Design", 30_000L, MilestoneStatus.PAID, now - (30 * dayMs)),
                Milestone("m2", "Build", 45_000L, MilestoneStatus.PAID, now - (15 * dayMs)),
                Milestone("m3", "Launch", 30_000L, MilestoneStatus.INVOICED, now - (5 * dayMs)),
                Milestone("m4", "Support", 15_000L, MilestoneStatus.PLANNED, now + (20 * dayMs))
            )

            val clinicMilestones = listOf(
                Milestone("c1", "Booking Engine", 50_000L, MilestoneStatus.INVOICED, now - (2 * dayMs)),
                Milestone("c2", "Patient Portal", 70_000L, MilestoneStatus.PLANNED, now + (15 * dayMs))
            )

            return listOf(
                Project("p1", "Bakery website", "React build · Sep–Nov 2026", 120_000L, bakeryMilestones),
                Project("p2", "Clinic booking site", "Next.js · Oct–Dec 2026", 120_000L, clinicMilestones)
            )
        }
    }
}
