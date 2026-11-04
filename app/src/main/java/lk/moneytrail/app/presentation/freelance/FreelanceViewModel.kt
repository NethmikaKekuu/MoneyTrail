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

    private var previousProjectState: Project? = null
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

    /** Opens edit draft state for a milestone without mutating list until save */
    fun onMilestoneClicked(milestone: Milestone) {
        _uiState.update {
            it.copy(
                editingMilestone = milestone,
                draftTitle = milestone.title,
                draftAmountDigits = milestone.amount.toString(),
                errorMessage = null
            )
        }
    }

    fun onDraftTitleChange(title: String) {
        _uiState.update { it.copy(draftTitle = title, errorMessage = null) }
    }

    fun onDraftAmountChange(digits: String) {
        _uiState.update { it.copy(draftAmountDigits = digits.filter(Char::isDigit), errorMessage = null) }
    }

    fun onDismissEditMilestone() {
        _uiState.update {
            it.copy(
                editingMilestone = null,
                draftTitle = "",
                draftAmountDigits = "",
                errorMessage = null
            )
        }
    }

    /** Replaces milestone by ID with new values preserving its existing status (PLANNED, INVOICED, or PAID) */
    fun onSaveEditedMilestone() {
        val state = _uiState.value
        val targetMilestone = state.editingMilestone ?: return
        val selectedProject = state.selectedProject ?: return

        val newTitle = state.draftTitle.trim()
        val newAmount = state.draftAmountDigits.toLongOrNull() ?: 0L

        if (newTitle.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter a milestone title.") }
            return
        }
        if (newAmount <= 0) {
            _uiState.update { it.copy(errorMessage = "Enter a valid milestone amount.") }
            return
        }

        previousProjectState = selectedProject

        val updatedMilestones = selectedProject.milestones.map { milestone ->
            if (milestone.id == targetMilestone.id) {
                milestone.copy(
                    title = newTitle,
                    amount = newAmount
                )
            } else milestone
        }

        val updatedProject = selectedProject.copy(
            milestones = updatedMilestones,
            totalBudget = updatedMilestones.sumOf { it.amount }
        )

        _uiState.update {
            it.copy(
                editingMilestone = null,
                draftTitle = "",
                draftAmountDigits = ""
            )
        }

        saveProjectUpdate(updatedProject, undoMessage = "Milestone '$newTitle' updated.")
    }

    /** Finds the first PLANNED milestone of selected project and transitions it to INVOICED */
    fun onMarkInvoicedClicked() {
        val selected = _uiState.value.selectedProject ?: return
        val milestoneToInvoice = selected.milestones.find { it.status == MilestoneStatus.PLANNED } ?: return
        previousProjectState = selected

        val updatedMilestones = selected.milestones.map {
            if (it.id == milestoneToInvoice.id) it.copy(status = MilestoneStatus.INVOICED) else it
        }
        val updatedProject = selected.copy(milestones = updatedMilestones)
        saveProjectUpdate(updatedProject, undoMessage = "Marked '${milestoneToInvoice.title}' as Invoiced")
    }

    /** Finds the first INVOICED milestone of selected project and transitions it to PAID */
    fun onRecordPaymentClicked() {
        val selected = _uiState.value.selectedProject ?: return
        val milestoneToPay = selected.milestones.find { it.status == MilestoneStatus.INVOICED }
            ?: selected.milestones.find { it.status == MilestoneStatus.PLANNED }
            ?: return
        previousProjectState = selected

        val updatedMilestones = selected.milestones.map {
            if (it.id == milestoneToPay.id) it.copy(status = MilestoneStatus.PAID) else it
        }
        val updatedProject = selected.copy(milestones = updatedMilestones)
        saveProjectUpdate(updatedProject, undoMessage = "Marked '${milestoneToPay.title}' as Paid")
    }

    /** Reverts an individual milestone's status: PAID -> INVOICED, INVOICED -> PLANNED */
    fun onUndoMilestoneClicked(milestoneId: String) {
        val selected = _uiState.value.selectedProject ?: return
        val targetMilestone = selected.milestones.find { it.id == milestoneId } ?: return
        if (targetMilestone.status == MilestoneStatus.PLANNED) return
        previousProjectState = selected

        val newStatus = when (targetMilestone.status) {
            MilestoneStatus.PAID -> MilestoneStatus.INVOICED
            MilestoneStatus.INVOICED -> MilestoneStatus.PLANNED
            else -> return
        }

        val updatedMilestones = selected.milestones.map {
            if (it.id == milestoneId) it.copy(status = newStatus) else it
        }
        val updatedProject = selected.copy(milestones = updatedMilestones)
        saveProjectUpdate(updatedProject, undoMessage = "Reverted status for '${targetMilestone.title}'")
    }

    /** Deletes a project by id */
    fun onDeleteProject(projectId: String) {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            freelanceRepository.deleteProject(projectId)
                .onSuccess {
                    _uiState.update { state ->
                        val updatedList = state.projects.filterNot { it.id == projectId }
                        state.copy(
                            isSaving = false,
                            projects = updatedList,
                            selectedProjectId = updatedList.firstOrNull()?.id,
                            undoMessage = "Project deleted."
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = error.message) }
                }
        }
    }

    // Aliases for backwards compatibility
    fun onMarkInvoiced() = onMarkInvoicedClicked()
    fun onRecordPayment() = onRecordPaymentClicked()
    fun onRevertMilestoneStatus(milestoneId: String) = onUndoMilestoneClicked(milestoneId)

    /** Undoes the last milestone status change by restoring the previous project state */
    fun onUndoLastAction() {
        val projectToRestore = previousProjectState ?: return
        previousProjectState = null

        _uiState.update { it.copy(isSaving = true, undoMessage = null) }
        viewModelScope.launch {
            freelanceRepository.saveProject(projectToRestore)
                .onSuccess {
                    _uiState.update { state ->
                        val updatedList = state.projects.map { if (it.id == projectToRestore.id) projectToRestore else it }
                        state.copy(isSaving = false, projects = updatedList, errorMessage = null)
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = error.message) }
                }
        }
    }

    fun onDismissUndo() {
        _uiState.update { it.copy(undoMessage = null) }
    }

    private fun saveProjectUpdate(project: Project, undoMessage: String? = null) {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            freelanceRepository.saveProject(project)
                .onSuccess {
                    _uiState.update { state ->
                        val updatedList = state.projects.map { if (it.id == project.id) project else it }
                        state.copy(isSaving = false, projects = updatedList, undoMessage = undoMessage)
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = error.message) }
                }
        }
    }
}
