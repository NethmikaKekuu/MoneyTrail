package lk.moneytrail.app.presentation.salary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.moneytrail.app.domain.repository.PlanRepository
import lk.moneytrail.app.domain.usecase.UpdateSalaryUseCase
import java.time.LocalDate
import javax.inject.Inject

/**
 * The pay day and base salary that onboarding set. The salary itself is confirmed on the
 * calendar ("Did it come in?"), where anything above the base is the month's bonus.
 */
@HiltViewModel
class SalaryViewModel @Inject constructor(
    private val planRepository: PlanRepository,
    private val updateSalary: UpdateSalaryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalaryState())
    val uiState: StateFlow<SalaryState> = _uiState.asStateFlow()

    init {
        loadPlan()
    }

    fun loadPlan() {
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            planRepository.getPlan()
                .onSuccess { plan ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasPlan = plan != null,
                            payDay = plan?.payDay,
                            baseSalary = plan?.baseSalary ?: 0L,
                            selectedPayDay = plan?.payDay,
                            baseSalaryDigits = plan?.baseSalary?.takeIf { base -> base > 0 }?.toString().orEmpty()
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, loadError = e.message ?: "Couldn't load your salary.") }
                }
        }
    }

    fun onPayDayClick(day: Int) {
        _uiState.update { it.copy(selectedPayDay = day, errorMessage = null, isSaved = false) }
    }

    fun onBaseSalaryChange(text: String) {
        val digits = text.filter(Char::isDigit).take(MAX_DIGITS)
        _uiState.update { it.copy(baseSalaryDigits = digits, errorMessage = null, isSaved = false) }
    }

    /** Saves the pay day and base salary; this month's expected salary follows (UpdateSalaryUseCase). */
    fun onSaveClick() {
        val state = _uiState.value
        if (state.isSaving) return

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            updateSalary(state.selectedPayDay, state.baseSalaryDigits.toLongOrNull(), LocalDate.now())
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isSaved = true,
                            payDay = state.selectedPayDay,
                            baseSalary = state.baseSalaryDigits.toLongOrNull() ?: 0L
                        )
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, errorMessage = e.message) } }
        }
    }

    private companion object {
        /** LKR 999,999,999 at most, as everywhere else in the app. */
        const val MAX_DIGITS = 9
    }
}
