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
import lk.moneytrail.app.domain.model.monthlySavings
import lk.moneytrail.app.domain.model.savingsDayIn
import lk.moneytrail.app.domain.repository.EntryRepository
import lk.moneytrail.app.domain.repository.PlanRepository
import lk.moneytrail.app.domain.usecase.DeleteGoalUseCase
import lk.moneytrail.app.domain.usecase.MoveSavingsUseCase
import lk.moneytrail.app.domain.usecase.RemindSavingsLaterUseCase
import lk.moneytrail.app.domain.usecase.SaveGoalUseCase
import lk.moneytrail.app.presentation.components.formatAmount
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Pay yourself first (MoneyTrail-Figma-HTML/14-SavingsTransfer.html): every goal's
 * progress, this month's saving to move, and adding, editing and removing goals. The
 * rules live in the use cases; this only turns taps into calls and results into state.
 */
@HiltViewModel
class GoalViewModel @Inject constructor(
    private val planRepository: PlanRepository,
    private val entryRepository: EntryRepository,
    private val saveGoal: SaveGoalUseCase,
    private val deleteGoal: DeleteGoalUseCase,
    private val moveSavings: MoveSavingsUseCase,
    private val remindSavingsLater: RemindSavingsLaterUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<GoalUiState>(GoalUiState.Loading)
    val uiState: StateFlow<GoalUiState> = _uiState.asStateFlow()

    private var currentPlan: MoneyPlan? = null
    private var observeJob: Job? = null
    private var salaryJob: Job? = null

    /** "Salary confirmed: …" for this month's confirmed salary; null until it's confirmed. */
    private var salaryConfirmedText: String? = null

    /** The account this month's salary went into, to say where to move savings from. */
    private var salaryAccount: String? = null

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
                    val salary = entries.lastOrNull { it.isSalary && !it.isExpected }
                    salaryAccount = salary?.account
                    salaryConfirmedText = salary?.let {
                        val bonus = if (it.bonus > 0) " (bonus ${formatAmount(it.bonus)})" else ""
                        "Salary confirmed: LKR ${formatAmount(it.amount)}$bonus"
                    }
                    updateSuccess { copy(salaryConfirmedText = salaryConfirmedText, fromAccount = salaryAccount) }
                }
            }
        }
    }

    /** Listens to the plan, so progress updates the moment savings are moved or a goal changes. */
    private fun observeGoalProgress() {
        observeJob?.cancel()
        _uiState.value = GoalUiState.Loading

        observeJob = viewModelScope.launch {
            planRepository.observePlan().collect { result ->
                result
                    .onSuccess { plan ->
                        currentPlan = plan
                        if (plan == null || plan.goals.isEmpty()) {
                            _uiState.value = GoalUiState.Empty
                        } else {
                            val today = LocalDate.now()
                            val savingsToMove = plan.monthlySavings(today)
                            val previousState = _uiState.value as? GoalUiState.Success
                            _uiState.value = (previousState ?: GoalUiState.Success(goals = emptyList())).copy(
                                goals = plan.goals.map { GoalModel.fromSavingsGoal(it, today) },
                                salaryConfirmedText = salaryConfirmedText,
                                fromAccount = salaryAccount,
                                savingsToMove = savingsToMove,
                                totalAllocationPool = savingsToMove.values.sum(),
                                isSavingsMovedThisMonth = plan.savingsMovedMonth == YearMonth.from(today),
                                nextSavingsDate = plan.savingsDayIn(YearMonth.from(today).plusMonths(1)),
                                payDay = plan.payDay
                            )
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
        updateSuccess { copy(isNewGoalDialogOpen = true, editingGoal = null) }
    }

    fun onEditGoalClick(goalId: String) {
        val goal = currentPlan?.goals?.find { it.id == goalId } ?: return
        updateSuccess { copy(isNewGoalDialogOpen = true, editingGoal = goal) }
    }

    fun onDismissNewGoalDialog() {
        updateSuccess { copy(isNewGoalDialogOpen = false, editingGoal = null) }
    }

    /** "I've moved it": asks how much went to each goal, starting from this month's saving. */
    fun onOpenAllocationDialog() {
        updateSuccess { copy(isAllocationDialogOpen = true) }
    }

    fun onDismissAllocationDialog() {
        updateSuccess { copy(isAllocationDialogOpen = false) }
    }

    fun onSaveNewGoal(
        name: String,
        targetAmount: Long,
        savedAmount: Long,
        monthsToDeadline: Int,
        transferDay: Int,
        allowEmergency: Boolean
    ) {
        val editing = (_uiState.value as? GoalUiState.Success)?.editingGoal
        val goal = SavingsGoal(
            name = name,
            targetAmount = targetAmount,
            savedAmount = savedAmount,
            targetDate = LocalDate.now().plusMonths(monthsToDeadline.toLong()),
            transferDay = transferDay,
            allowEmergencyWithdrawals = allowEmergency,
            id = editing?.id.orEmpty()
        )
        // The same checks the use case makes, so the dialog stays open with the reason.
        SaveGoalUseCase.goalError(goal)?.let { message ->
            updateSuccess { copy(bannerMessage = message) }
            return
        }

        updateSuccess { copy(isNewGoalDialogOpen = false, editingGoal = null, isTransferring = true) }
        viewModelScope.launch {
            saveGoal(goal)
                .onSuccess { finish("Goal '${goal.name.trim()}' saved.") }
                .onFailure { error -> finish(error.message ?: "Couldn't save the goal.") }
        }
    }

    /** The user moved money to their savings account: LKR by goal id. */
    fun onConfirmSalaryAllocation(allocations: Map<String, Long>) {
        updateSuccess { copy(isTransferring = true, isAllocationDialogOpen = false, bannerMessage = null) }
        viewModelScope.launch {
            moveSavings(allocations, LocalDate.now())
                .onSuccess { moved -> finish("LKR ${formatAmount(moved)} added to your goals. Done for this month.") }
                .onFailure { error -> finish(error.message ?: "Couldn't update your goals.") }
        }
    }

    /** "Remind me tomorrow": the reminder on the calendar comes back tomorrow. */
    fun onRemindTomorrowClick() {
        viewModelScope.launch {
            remindSavingsLater(LocalDate.now())
                .onSuccess { updateSuccess { copy(isReminderSet = true) } }
                .onFailure { error -> updateSuccess { copy(bannerMessage = error.message) } }
        }
    }

    /** The screen has left after "Remind me tomorrow". */
    fun onReminderHandled() {
        updateSuccess { copy(isReminderSet = false) }
    }

    fun onDeleteGoalClick(goalId: String) {
        updateSuccess { copy(deletingGoalId = goalId) }
    }

    fun onDeleteGoalDismiss() {
        updateSuccess { copy(deletingGoalId = null) }
    }

    fun onDeleteGoalConfirm() {
        val state = _uiState.value as? GoalUiState.Success ?: return
        val goalId = state.deletingGoalId ?: return
        val name = state.goals.find { it.id == goalId }?.name.orEmpty()

        updateSuccess { copy(deletingGoalId = null, isTransferring = true, bannerMessage = null) }
        viewModelScope.launch {
            deleteGoal(goalId)
                .onSuccess { finish("Goal '$name' removed.") }
                .onFailure { error -> finish(error.message ?: "Couldn't remove the goal.") }
        }
    }

    fun onDismissBanner() {
        updateSuccess { copy(bannerMessage = null) }
    }

    private fun finish(message: String) {
        updateSuccess { copy(isTransferring = false, bannerMessage = message) }
    }

    private fun updateSuccess(change: GoalUiState.Success.() -> GoalUiState.Success) {
        _uiState.update { if (it is GoalUiState.Success) it.change() else it }
    }
}
