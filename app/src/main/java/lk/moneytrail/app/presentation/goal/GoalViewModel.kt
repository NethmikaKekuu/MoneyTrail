package lk.moneytrail.app.presentation.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.moneytrail.app.domain.model.GoalModel
import lk.moneytrail.app.domain.model.MoneyPlan
import lk.moneytrail.app.domain.model.SavingsGoal
import lk.moneytrail.app.domain.repository.EntryRepository
import lk.moneytrail.app.domain.repository.PlanRepository
import lk.moneytrail.app.presentation.components.formatAmount
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class GoalViewModel @Inject constructor(
    private val planRepository: PlanRepository,
    private val entryRepository: EntryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<GoalUiState>(GoalUiState.Loading)
    val uiState: StateFlow<GoalUiState> = _uiState.asStateFlow()

    private var currentPlan: MoneyPlan? = null
    private var observeJob: Job? = null
    private var salaryJob: Job? = null

    /** "Salary confirmed: …" for this month's confirmed salary; null until it's confirmed. */
    private var salaryConfirmedText: String? = null

    init {
        observeGoalProgress()
        observeThisMonthsSalary()
    }

    fun onRetry() {
        observeGoalProgress()
        observeThisMonthsSalary()
    }

    /** Follows this month's entries, so the banner shows the salary as it really arrived. */
    private fun observeThisMonthsSalary() {
        salaryJob?.cancel()
        salaryJob = viewModelScope.launch {
            entryRepository.observeMonth(YearMonth.now()).collect { result ->
                // Only the banner depends on this, so a failure just leaves it out.
                result.onSuccess { entries ->
                    salaryConfirmedText = entries.lastOrNull { it.isSalary && !it.isExpected }?.let { salary ->
                        val bonus = if (salary.bonus > 0) " (bonus ${formatAmount(salary.bonus)})" else ""
                        "Salary confirmed: LKR ${formatAmount(salary.amount)}$bonus"
                    }
                    _uiState.update { state ->
                        if (state is GoalUiState.Success) state.copy(salaryConfirmedText = salaryConfirmedText) else state
                    }
                }
            }
        }
    }

    /**
     * Listens to Firestore real-time updates for the plan & multi-goals.
     * Dynamically computes remaining shortfalls and recommended savings across all active goals.
     */
    private fun observeGoalProgress() {
        observeJob?.cancel()
        _uiState.value = GoalUiState.Loading

        observeJob = viewModelScope.launch {
            planRepository.observePlan().collect { result ->
                result
                    .onSuccess { plan ->
                        currentPlan = plan
                        if (plan == null || plan.goals.isEmpty() || plan.goals.all { it.targetAmount <= 0 }) {
                            _uiState.value = GoalUiState.Empty
                        } else {
                            val today = LocalDate.now()
                            val goalModels = plan.goals.map { GoalModel.fromSavingsGoal(it, today) }
                            val totalMonthlySaving = goalModels.sumOf { it.monthlySavingNeeded }

                            val previousState = _uiState.value

                            if (previousState is GoalUiState.Success) {
                                _uiState.value = previousState.copy(
                                    goals = goalModels,
                                    salaryConfirmedText = salaryConfirmedText,
                                    totalAllocationPool = totalMonthlySaving,
                                    payDay = plan.payDay
                                )
                            } else {
                                _uiState.value = GoalUiState.Success(
                                    goals = goalModels,
                                    salaryConfirmedText = salaryConfirmedText,
                                    totalAllocationPool = totalMonthlySaving,
                                    payDay = plan.payDay
                                )
                            }
                        }
                    }
                    .onFailure { error ->
                        _uiState.value = GoalUiState.Error(
                            message = error.message ?: "Couldn't load your goal progress."
                        )
                    }
            }
        }
    }

    fun onOpenNewGoalDialog() {
        _uiState.update {
            if (it is GoalUiState.Success) {
                it.copy(isNewGoalDialogOpen = true)
            } else it
        }
    }

    fun onDismissNewGoalDialog() {
        _uiState.update {
            if (it is GoalUiState.Success) {
                it.copy(isNewGoalDialogOpen = false)
            } else it
        }
    }

    fun onOpenAllocationDialog() {
        _uiState.update {
            if (it is GoalUiState.Success) {
                it.copy(isAllocationDialogOpen = true)
            } else it
        }
    }

    fun onDismissAllocationDialog() {
        _uiState.update {
            if (it is GoalUiState.Success) {
                it.copy(isAllocationDialogOpen = false)
            } else it
        }
    }

    fun onSaveNewGoal(
        name: String,
        targetAmount: Long,
        savedAmount: Long,
        monthsToDeadline: Int,
        transferDay: Int,
        allowEmergency: Boolean
    ) {
        val plan = currentPlan ?: return

        _uiState.update {
            if (it is GoalUiState.Success) {
                it.copy(isNewGoalDialogOpen = false, isTransferring = true)
            } else it
        }

        viewModelScope.launch {
            val targetDate = LocalDate.now().plusMonths(monthsToDeadline.toLong())
            val newGoal = SavingsGoal(
                name = name,
                targetAmount = targetAmount,
                savedAmount = savedAmount,
                targetDate = targetDate,
                transferDay = transferDay,
                allowEmergencyWithdrawals = allowEmergency
            )

            val existingIndex = plan.goals.indexOfFirst { it.name.equals(name, ignoreCase = true) }
            val updatedGoalsList = if (existingIndex >= 0) {
                plan.goals.toMutableList().apply { set(existingIndex, newGoal) }
            } else {
                plan.goals + newGoal
            }

            val updatedPlan = plan.copy(goals = updatedGoalsList)

            planRepository.savePlan(updatedPlan)
                .onSuccess {
                    _uiState.update { state ->
                        if (state is GoalUiState.Success) {
                            state.copy(
                                isTransferring = false,
                                bannerMessage = "Goal '$name' saved successfully!"
                            )
                        } else state
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        if (state is GoalUiState.Success) {
                            state.copy(
                                isTransferring = false,
                                bannerMessage = error.message ?: "Failed to save goal"
                            )
                        } else state
                    }
                }
        }
    }

    /**
     * Distributes salary savings across multiple active goals.
     * Updates each goal's `savedAmount` in Firestore according to its allocated portion.
     */
    fun onConfirmSalaryAllocation(allocations: Map<String, Long>) {
        val plan = currentPlan ?: return
        if (allocations.isEmpty()) return

        _uiState.update {
            if (it is GoalUiState.Success) {
                it.copy(isTransferring = true, isAllocationDialogOpen = false, bannerMessage = null)
            } else it
        }

        viewModelScope.launch {
            val updatedGoals = plan.goals.map { goal ->
                val allocatedAmount = allocations[goal.name] ?: 0L
                if (allocatedAmount > 0) {
                    goal.copy(savedAmount = goal.savedAmount + allocatedAmount)
                } else goal
            }

            val updatedPlan = plan.copy(goals = updatedGoals)
            val totalAllocated = allocations.values.sum()

            planRepository.savePlan(updatedPlan)
                .onSuccess {
                    _uiState.update { state ->
                        if (state is GoalUiState.Success) {
                            state.copy(
                                isTransferring = false,
                                bannerMessage = "Allocated LKR ${formatAmount(totalAllocated)} across goals!"
                            )
                        } else state
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        if (state is GoalUiState.Success) {
                            state.copy(
                                isTransferring = false,
                                bannerMessage = error.message ?: "Failed to allocate salary"
                            )
                        } else state
                    }
                }
        }
    }

    /**
     * Removes a goal by name from Firestore.
     */
    fun onDeleteGoal(goalName: String) {
        val plan = currentPlan ?: return
        val updatedGoals = plan.goals.filterNot { it.name.equals(goalName, ignoreCase = true) }
        if (updatedGoals.isEmpty()) return

        _uiState.update {
            if (it is GoalUiState.Success) {
                it.copy(isTransferring = true, bannerMessage = null)
            } else it
        }

        viewModelScope.launch {
            val updatedPlan = plan.copy(goals = updatedGoals)
            planRepository.savePlan(updatedPlan)
                .onSuccess {
                    _uiState.update { state ->
                        if (state is GoalUiState.Success) {
                            state.copy(
                                isTransferring = false,
                                bannerMessage = "Goal '$goalName' removed."
                            )
                        } else state
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        if (state is GoalUiState.Success) {
                            state.copy(
                                isTransferring = false,
                                bannerMessage = error.message ?: "Failed to delete goal"
                            )
                        } else state
                    }
                }
        }
    }

    fun onDismissBanner() {
        _uiState.update {
            if (it is GoalUiState.Success) {
                it.copy(bannerMessage = null)
            } else it
        }
    }
}
