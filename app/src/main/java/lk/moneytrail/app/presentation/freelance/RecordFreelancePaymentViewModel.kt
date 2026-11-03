package lk.moneytrail.app.presentation.freelance

import lk.moneytrail.app.presentation.navigation.Routes
import androidx.lifecycle.SavedStateHandle
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
    savedStateHandle: SavedStateHandle,
    private val freelanceRepository: FreelanceRepository,
    private val entryRepository: EntryRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordFreelancePaymentUiState())
    val uiState: StateFlow<RecordFreelancePaymentUiState> = _uiState.asStateFlow()

    private var allProjects: List<Project> = emptyList()

    /** Set when opened from a project: start on that project's next unpaid milestone. */
    private val startProjectId: String? = savedStateHandle[Routes.RECORD_PAYMENT_PROJECT_ARG]

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            freelanceRepository.observeProjects().collect { result ->
                result.onSuccess { projects ->
                    allProjects = projects
                    updateMilestoneOptions(allProjects)
                }
            }
        }

        viewModelScope.launch {
            accountRepository.observeAccounts().collect { result ->
                result.onSuccess { accountsList ->
                    _uiState.update { state ->
                        state.copy(
                            accounts = accountsList,
                            // Keep the user's choice when the list updates; start on the first account.
                            selectedAccount = state.selectedAccount.takeIf { name -> accountsList.any { it.name == name } }
                                ?: accountsList.firstOrNull()?.name.orEmpty()
                        )
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
                    options.add(
                        UnpaidMilestoneOption(
                            optionId = milestone.id,
                            milestone = milestone,
                            project = project,
                            title = milestone.title,
                            amount = milestone.amount,
                            subtext = "${project.name} · ${milestone.dueText(LocalDate.now())}"
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
            // Keep what the user picked; the first time, start on the project they came from.
            val keep = options.find { it.optionId == state.selectedOptionId }
            val start = keep
                ?: projects.find { it.id == startProjectId }?.nextUnpaidMilestone()?.let { m -> options.find { it.optionId == m.id } }
                ?: options.first()
            state.copy(
                milestoneOptions = options,
                selectedOptionId = start.optionId,
                amountDigits = if (keep != null || state.amountDigits.isNotEmpty()) state.amountDigits
                else start.amount.takeIf { it > 0 }?.toString().orEmpty()
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
        if (state.selectedAccount.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Pick the account the money went into.") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val selectedOption = state.selectedOption
            val milestone = selectedOption?.milestone
            val project = selectedOption?.project

            // 1. The money itself: a confirmed income entry on the calendar.
            val incomeEntry = Entry(
                id = "",
                type = EntryType.INCOME,
                amount = amount,
                category = "Freelance",
                account = state.selectedAccount,
                date = state.selectedDate,
                note = if (milestone != null) {
                    "Freelance payment: ${milestone.title} (${project?.name.orEmpty()})"
                } else {
                    "Unlinked freelance income"
                },
                spendingType = null,
                status = EntryStatus.CONFIRMED
            )
            val saved = entryRepository.addEntry(incomeEntry)
            if (saved.isFailure) {
                fail(saved.exceptionOrNull()?.message ?: "Couldn't save the payment.")
                return@launch
            }

            // 2. Only then mark the milestone paid, so a failed save never shows it as paid.
            if (milestone != null && project != null) {
                val updatedProject = project.copy(
                    milestones = project.milestones.map { if (it.id == milestone.id) it.copy(status = MilestoneStatus.PAID) else it }
                )
                val marked = freelanceRepository.saveProject(updatedProject)
                if (marked.isFailure) {
                    fail("The payment is saved, but ${milestone.title} couldn't be marked paid. Try again from the project.")
                    return@launch
                }
            }

            _uiState.update { it.copy(isSaving = false) }
            onSuccess()
        }
    }

    private fun fail(message: String) {
        _uiState.update { it.copy(isSaving = false, errorMessage = message) }
    }
}
