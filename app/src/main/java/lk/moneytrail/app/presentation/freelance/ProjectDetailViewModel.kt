package lk.moneytrail.app.presentation.freelance

import androidx.lifecycle.SavedStateHandle
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
import lk.moneytrail.app.domain.model.dueTimestampOf
import lk.moneytrail.app.domain.repository.FreelanceRepository
import lk.moneytrail.app.presentation.navigation.Routes
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/** One project: its milestones and the invoice → payment steps for each. */
@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val freelanceRepository: FreelanceRepository
) : ViewModel() {

    private val projectId: String = savedStateHandle[Routes.PROJECT_ID_ARG] ?: ""

    private val _uiState = MutableStateFlow(ProjectDetailUiState())
    val uiState: StateFlow<ProjectDetailUiState> = _uiState.asStateFlow()

    /** The project before the last change, for Undo. */
    private var beforeLastChange: Project? = null
    private var observeJob: Job? = null
    private var deleted = false

    init {
        observe()
    }

    fun onRetry() = observe()

    private fun observe() {
        observeJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        observeJob = viewModelScope.launch {
            freelanceRepository.observeProjects().collect { result ->
                if (deleted) return@collect
                result
                    .onSuccess { projects ->
                        val project = projects.find { it.id == projectId }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                project = project,
                                today = LocalDate.now(),
                                loadError = if (project == null) "This project no longer exists." else null
                            )
                        }
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(isLoading = false, loadError = error.message ?: "Couldn't load the project.") }
                    }
            }
        }
    }

    /** Planned → invoiced: the client has been asked to pay. */
    fun onMarkInvoiced(milestoneId: String) =
        changeStatus(milestoneId, from = MilestoneStatus.PLANNED, to = MilestoneStatus.INVOICED, "marked invoiced")

    /** Invoiced → planned, if it was invoiced by mistake. */
    fun onUndoInvoiced(milestoneId: String) =
        changeStatus(milestoneId, from = MilestoneStatus.INVOICED, to = MilestoneStatus.PLANNED, "moved back to planned")

    private fun changeStatus(milestoneId: String, from: MilestoneStatus, to: MilestoneStatus, done: String) {
        val project = _uiState.value.project ?: return
        val milestone = project.milestones.find { it.id == milestoneId }?.takeIf { it.status == from } ?: return
        save(
            project.copy(milestones = project.milestones.map { if (it.id == milestoneId) it.copy(status = to) else it }),
            undoMessage = "${milestone.title} $done."
        )
    }

    fun onOpenAddMilestone() {
        if (_uiState.value.canAddMilestone) _uiState.update { it.copy(isAddMilestoneOpen = true) }
    }

    fun onDismissAddMilestone() = _uiState.update { it.copy(isAddMilestoneOpen = false) }

    fun onAddMilestone(input: MilestoneInput) {
        val project = _uiState.value.project ?: return
        _uiState.update { it.copy(isAddMilestoneOpen = false) }
        val milestone = Milestone(
            id = UUID.randomUUID().toString(),
            title = input.title,
            amount = input.amount,
            status = MilestoneStatus.PLANNED,
            dueTimestamp = dueTimestampOf(input.dueDate)
        )
        val milestones = project.milestones + milestone
        // An extra milestone on a fully split project is extra work, so the price grows with it.
        val price = maxOf(project.totalBudget, milestones.sumOf { it.amount })
        val note = if (price > project.totalBudget) " Price is now ${lk.moneytrail.app.presentation.components.formatAmount(price)}." else ""
        save(project.copy(milestones = milestones, totalBudget = price), undoMessage = "${input.title} added.$note")
    }

    fun onEditMilestone(milestone: Milestone) {
        // A paid milestone is already on the calendar as income; it stays as it is.
        if (milestone.status != MilestoneStatus.PAID) _uiState.update { it.copy(editingMilestone = milestone) }
    }

    fun onDismissEditMilestone() = _uiState.update { it.copy(editingMilestone = null) }

    /** Title, amount and due date change; the status and the project price stay. */
    fun onSaveEditedMilestone(input: MilestoneInput) {
        val state = _uiState.value
        val project = state.project ?: return
        val target = state.editingMilestone ?: return
        _uiState.update { it.copy(editingMilestone = null) }
        save(
            project.copy(milestones = project.milestones.map {
                if (it.id == target.id) it.copy(title = input.title, amount = input.amount, dueTimestamp = dueTimestampOf(input.dueDate)) else it
            }),
            undoMessage = "${input.title} updated."
        )
    }

    fun onRemoveMilestone(milestone: Milestone) {
        val project = _uiState.value.project ?: return
        if (milestone.status == MilestoneStatus.PAID) return
        _uiState.update { it.copy(editingMilestone = null) }
        save(project.copy(milestones = project.milestones.filterNot { it.id == milestone.id }), undoMessage = "${milestone.title} removed.")
    }

    fun onAskDelete() = _uiState.update { it.copy(isDeleteConfirmOpen = true) }
    fun onDismissDelete() = _uiState.update { it.copy(isDeleteConfirmOpen = false) }

    fun onConfirmDelete(onDeleted: () -> Unit) {
        _uiState.update { it.copy(isDeleteConfirmOpen = false, isSaving = true) }
        viewModelScope.launch {
            freelanceRepository.deleteProject(projectId)
                .onSuccess {
                    deleted = true
                    onDeleted()
                }
                .onFailure { error -> _uiState.update { it.copy(isSaving = false, message = error.message ?: "Couldn't delete the project.") } }
        }
    }

    fun onUndo() {
        val previous = beforeLastChange ?: return
        beforeLastChange = null
        _uiState.update { it.copy(undoMessage = null) }
        save(previous, undoMessage = null, rememberForUndo = false)
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null, undoMessage = null) }

    private fun save(updated: Project, undoMessage: String?, rememberForUndo: Boolean = true) {
        val before = _uiState.value.project ?: return
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, project = updated) }
        viewModelScope.launch {
            freelanceRepository.saveProject(updated)
                .onSuccess {
                    if (rememberForUndo) beforeLastChange = before
                    _uiState.update { it.copy(isSaving = false, undoMessage = undoMessage) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSaving = false, project = before, message = error.message ?: "Couldn't save the change.") }
                }
        }
    }
}
