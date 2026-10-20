package lk.moneytrail.app.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.moneytrail.app.domain.model.IncomeSource
import lk.moneytrail.app.domain.model.MoneyPlan
import lk.moneytrail.app.domain.model.SavingsGoal
import lk.moneytrail.app.domain.usecase.CompleteOnboardingUseCase
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val completeOnboarding: CompleteOnboardingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingState(today = LocalDate.now()))
    val uiState: StateFlow<OnboardingState> = _uiState.asStateFlow()

    fun onAccountToggle(id: String) {
        _uiState.update {
            val ids = if (id in it.selectedAccountIds) it.selectedAccountIds - id else it.selectedAccountIds + id
            it.copy(selectedAccountIds = ids, errorMessage = null)
        }
    }

    fun onPayDayClick(day: Int) {
        _uiState.update { it.copy(payDay = day, errorMessage = null) }
    }

    fun onBaseSalaryChange(text: String) {
        _uiState.update { it.copy(baseSalaryDigits = digitsOnly(text), errorMessage = null) }
    }

    fun onIncomeSourceToggle(source: IncomeSource) {
        _uiState.update {
            val sources = if (source in it.incomeSources) it.incomeSources - source else it.incomeSources + source
            it.copy(incomeSources = sources, errorMessage = null)
        }
    }

    fun onGoalNameChange(name: String) {
        _uiState.update {
            it.copy(goalName = name.take(CompleteOnboardingUseCase.MAX_GOAL_NAME_LENGTH), errorMessage = null)
        }
    }

    fun onGoalTargetChange(text: String) {
        _uiState.update { it.copy(goalTargetDigits = digitsOnly(text), errorMessage = null) }
    }

    fun onGoalSavedChange(text: String) {
        _uiState.update { it.copy(goalSavedDigits = digitsOnly(text), errorMessage = null) }
    }

    fun onGoalDateChange(date: LocalDate) {
        _uiState.update { it.copy(goalDate = date, errorMessage = null) }
    }

    /** Checks the step on screen, then moves to the next one, or saves everything on the last. */
    fun onContinueClick() {
        val state = _uiState.value
        if (state.isSaving) return

        val error = stepError(state)
        if (error != null) {
            _uiState.update { it.copy(errorMessage = error) }
            return
        }

        if (state.isLastStep) {
            finish(state)
        } else {
            _uiState.update { it.copy(step = OnboardingStep.entries[state.step.ordinal + 1], errorMessage = null) }
        }
    }

    /** Goes back one step. Does nothing on the first step. */
    fun onBackClick() {
        _uiState.update {
            if (it.step.ordinal == 0 || it.isSaving) it
            else it.copy(step = OnboardingStep.entries[it.step.ordinal - 1], errorMessage = null)
        }
    }

    private fun stepError(state: OnboardingState): String? = when (state.step) {
        OnboardingStep.MONEY_PLACES -> CompleteOnboardingUseCase.accountsError(state.selectedAccounts)
        OnboardingStep.SALARY -> CompleteOnboardingUseCase.salaryError(state.payDay, state.baseSalary)
        OnboardingStep.INCOME_SOURCES -> CompleteOnboardingUseCase.incomeSourcesError(state.incomeSources)
        OnboardingStep.GOAL -> CompleteOnboardingUseCase.goalError(
            name = state.goalName,
            targetAmount = state.goalTarget,
            savedAmount = state.goalSaved,
            targetDate = state.goalDate,
            today = state.today
        )
    }

    private fun finish(state: OnboardingState) {
        // Every step was checked on the way, so these are all present.
        val payDay = state.payDay ?: return
        val baseSalary = state.baseSalary ?: return
        val goalTarget = state.goalTarget ?: return

        val plan = MoneyPlan(
            payDay = payDay,
            baseSalary = baseSalary,
            incomeSources = state.incomeSources,
            goal = SavingsGoal(
                name = state.goalName.trim(),
                targetAmount = goalTarget,
                savedAmount = state.goalSaved,
                targetDate = state.goalDate
            )
        )

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            completeOnboarding(state.selectedAccounts, plan, state.today)
                .onSuccess { _uiState.update { it.copy(isSaving = false, isFinished = true) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, errorMessage = e.message) } }
        }
    }

    private fun digitsOnly(text: String): String = text.filter(Char::isDigit).take(MAX_DIGITS)

    private companion object {
        /** LKR 999,999,999 at most, as everywhere else in the app. */
        const val MAX_DIGITS = 9
    }
}
