package lk.moneytrail.app.presentation.freelance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddProjectViewModel @Inject constructor(
    private val freelanceRepository: FreelanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddProjectUiState())
    val uiState: StateFlow<AddProjectUiState> = _uiState.asStateFlow()

    fun onProjectNameChange(name: String) {
        _uiState.update { it.copy(projectName = name, errorMessage = null) }
    }

    fun onClientNameChange(client: String) {
        _uiState.update { it.copy(clientName = client, errorMessage = null) }
    }

    fun onTotalBudgetChange(digits: String) {
        _uiState.update { it.copy(totalBudgetDigits = digits.filter(Char::isDigit).take(12), errorMessage = null) }
    }

    fun onOpenAddMilestoneDialog() {
        val state = _uiState.value
        val error = when {
            state.totalProjectPrice <= 0 -> "Enter the total project price first."
            state.leftToSplit <= 0 -> "The whole price is already split into milestones."
            else -> null
        }
        _uiState.update { if (error != null) it.copy(errorMessage = error) else it.copy(isAddMilestoneDialogOpen = true, errorMessage = null) }
    }

    fun onDismissAddMilestoneDialog() {
        _uiState.update { it.copy(isAddMilestoneDialogOpen = false) }
    }

    fun onAddMilestone(input: MilestoneInput) {
        _uiState.update {
            it.copy(
                milestones = it.milestones + newMilestone(input.title, input.amount, input.dueDate),
                isAddMilestoneDialogOpen = false,
                errorMessage = null
            )
        }
    }

    fun onRemoveMilestone(index: Int) {
        _uiState.update { state ->
            state.copy(milestones = state.milestones.filterIndexed { i, _ -> i != index }, errorMessage = null)
        }
    }

    fun onSaveProject(onSuccess: (projectId: String) -> Unit) {
        val state = _uiState.value
        val error = state.saveError
        if (error != null) {
            _uiState.update { it.copy(errorMessage = error) }
            return
        }
        if (state.milestones.isNotEmpty() && state.leftToSplit > 0) {
            _uiState.update { it.copy(isShortSplitDialogOpen = true) }
            return
        }
        save(onSuccess)
    }

    /** From the "short of the price" question: add what's left as one last milestone, then save. */
    fun onAddRestAndSave(onSuccess: (projectId: String) -> Unit) {
        _uiState.update {
            it.copy(
                milestones = it.milestones + newMilestone("Final payment", it.leftToSplit, it.nextDueDate(LocalDate.now())),
                isShortSplitDialogOpen = false
            )
        }
        save(onSuccess)
    }

    fun onSaveAnyway(onSuccess: (projectId: String) -> Unit) {
        _uiState.update { it.copy(isShortSplitDialogOpen = false) }
        save(onSuccess)
    }

    fun onDismissShortSplitDialog() {
        _uiState.update { it.copy(isShortSplitDialogOpen = false) }
    }

    private fun save(onSuccess: (projectId: String) -> Unit) {
        val state = _uiState.value
        if (state.isSaving) return
        val project = buildNewProject(state, LocalDate.now(), UUID.randomUUID().toString())

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            freelanceRepository.saveProject(project)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false) }
                    onSuccess(project.id)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = error.message ?: "Couldn't save the project.") }
                }
        }
    }
}

private fun newMilestone(title: String, amount: Long, due: LocalDate) = Milestone(
    id = UUID.randomUUID().toString(),
    title = title,
    amount = amount,
    status = MilestoneStatus.PLANNED,
    dueTimestamp = dueTimestampOf(due)
)

/**
 * The project to save from the form. With no milestones the whole price becomes one
 * "Full payment" milestone, so it can still be invoiced and paid.
 */
internal fun buildNewProject(state: AddProjectUiState, today: LocalDate, id: String): Project = Project(
    id = id,
    name = state.projectName.trim(),
    client = state.clientName.trim(),
    totalBudget = state.totalProjectPrice,
    milestones = state.milestones.ifEmpty {
        listOf(newMilestone("Full payment", state.totalProjectPrice, state.nextDueDate(today)))
    }
)
