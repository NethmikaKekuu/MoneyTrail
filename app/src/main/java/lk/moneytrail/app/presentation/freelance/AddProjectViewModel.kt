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
import lk.moneytrail.app.domain.repository.FreelanceRepository
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
        _uiState.update { it.copy(totalBudgetDigits = digits.filter(Char::isDigit), errorMessage = null) }
    }

    fun onOpenAddMilestoneDialog() {
        _uiState.update { it.copy(isAddMilestoneDialogOpen = true) }
    }

    fun onDismissAddMilestoneDialog() {
        _uiState.update { it.copy(isAddMilestoneDialogOpen = false) }
    }

    fun onAddMilestone(title: String, amount: Long, dueTimestamp: Long) {
        val newMilestone = Milestone(
            id = UUID.randomUUID().toString(),
            title = title,
            amount = amount,
            status = MilestoneStatus.PLANNED,
            dueTimestamp = dueTimestamp
        )
        _uiState.update {
            it.copy(
                milestones = it.milestones + newMilestone,
                isAddMilestoneDialogOpen = false,
                errorMessage = null
            )
        }
    }

    fun onRemoveMilestone(index: Int) {
        _uiState.update { state ->
            val updated = state.milestones.toMutableList().apply {
                if (index in indices) removeAt(index)
            }
            state.copy(milestones = updated)
        }
    }

    fun onSaveProject(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.projectName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter a project name.") }
            return
        }
        if (state.clientName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter the client name.") }
            return
        }
        if (state.totalProjectPrice <= 0) {
            _uiState.update { it.copy(errorMessage = "Enter a total project price.") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val project = Project(
                name = state.projectName.trim(),
                client = state.clientName.trim(),
                totalBudget = state.totalProjectPrice,
                milestones = state.milestones
            )

            freelanceRepository.saveProject(project)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false) }
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Failed to save project."
                        )
                    }
                }
        }
    }
}
