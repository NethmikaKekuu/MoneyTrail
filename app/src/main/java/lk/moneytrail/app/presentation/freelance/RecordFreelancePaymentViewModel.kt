package lk.moneytrail.app.presentation.freelance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryStatus
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.MilestoneStatus
import lk.moneytrail.app.domain.model.Project
import lk.moneytrail.app.domain.repository.AccountRepository
import lk.moneytrail.app.domain.repository.EntryRepository
import lk.moneytrail.app.domain.repository.FreelanceRepository
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class RecordFreelancePaymentViewModel @Inject constructor(
    private val freelanceRepository: FreelanceRepository,
    private val entryRepository: EntryRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordFreelancePaymentUiState())
    val uiState: StateFlow<RecordFreelancePaymentUiState> = _uiState.asStateFlow()

    private var allProjects: List<Project> = emptyList()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            freelanceRepository.observeProjects().collect { result ->
                result.onSuccess { projects ->
                    allProjects = projects.ifEmpty { FreelanceViewModel.defaultMockProjects() }
                    updateMilestoneOptions(allProjects)
                }
            }
        }

        viewModelScope.launch {
            accountRepository.observeAccounts().collect { result ->
                result.onSuccess { accountsList ->
                    if (accountsList.isNotEmpty()) {
                        _uiState.update { state ->
                            state.copy(
                                accounts = accountsList,
                                selectedAccount = accountsList.first().name
                            )
                        }
                    }
                }
            }
        }
    }

    private fun updateMilestoneOptions(projects: List<Project>) {
        val options = mutableListOf<UnpaidMilestoneOption>()

        projects.forEach { project ->
            project.milestones
                .filter { it.status != MilestoneStatus.PAID }
                .forEach { milestone ->
                    val statusText = if (milestone.status == MilestoneStatus.INVOICED) "invoiced, due 14 Oct" else "overdue 12 days"
                    options.add(
                        UnpaidMilestoneOption(
                            optionId = milestone.id,
                            milestone = milestone,
                            project = project,
                            title = milestone.title,
                            amount = milestone.amount,
                            subtext = "${project.name} · $statusText"
                        )
                    )
                }
        }

        options.add(
            UnpaidMilestoneOption(
                optionId = "NO_MILESTONE",
                milestone = null,
                project = null,
                title = "No milestone",
                amount = 0L,
                subtext = "Save as unlinked freelance income"
            )
        )

        _uiState.update { state ->
            val firstOptionId = options.firstOrNull()?.optionId
            state.copy(
                milestoneOptions = options,
                selectedOptionId = state.selectedOptionId ?: firstOptionId,
                amountDigits = (options.firstOrNull()?.amount ?: 30000L).toString()
            )
        }
    }

    fun onAmountChange(digits: String) {
        _uiState.update { it.copy(amountDigits = digits.filter(Char::isDigit), errorMessage = null) }
    }

    fun onSelectOption(optionId: String) {
        _uiState.update { state ->
            val option = state.milestoneOptions.find { it.optionId == optionId }
            val newAmount = if (option?.amount != null && option.amount > 0) option.amount.toString() else state.amountDigits
            state.copy(selectedOptionId = optionId, amountDigits = newAmount, errorMessage = null)
        }
    }

    fun onSelectAccount(accountName: String) {
        _uiState.update { it.copy(selectedAccount = accountName, errorMessage = null) }
    }

    fun onSelectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date, errorMessage = null) }
    }

    fun onSavePayment(onSuccess: () -> Unit) {
        val state = _uiState.value
        val amount = state.amountValue

        if (amount <= 0) {
            _uiState.update { it.copy(errorMessage = "Enter an amount received.") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val selectedOption = state.selectedOption

            // 1. If linked to a milestone, mark that milestone as PAID on its project
            if (selectedOption != null && selectedOption.milestone != null && selectedOption.project != null) {
                val project = selectedOption.project
                val updatedMilestones = project.milestones.map {
                    if (it.id == selectedOption.milestone.id) it.copy(status = MilestoneStatus.PAID) else it
                }
                val updatedProject = project.copy(milestones = updatedMilestones)
                freelanceRepository.saveProject(updatedProject)
            }

            // 2. Automatically generate a confirmed INCOME entry for the calendar
            val incomeEntry = Entry(
                id = "",
                type = EntryType.INCOME,
                amount = amount,
                category = "Freelance",
                account = state.selectedAccount,
                date = state.selectedDate,
                note = if (selectedOption?.milestone != null) {
                    "Freelance payment: ${selectedOption.milestone.title} (${selectedOption.project?.name.orEmpty()})"
                } else {
                    "Unlinked freelance income"
                },
                spendingType = null,
                status = EntryStatus.CONFIRMED
            )

            entryRepository.addEntry(incomeEntry)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false) }
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Failed to save payment."
                        )
                    }
                }
        }
    }
}
