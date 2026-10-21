package lk.moneytrail.app.presentation.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.repository.EntryRepository
import lk.moneytrail.app.domain.repository.PlanRepository
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class GoalProgressViewModel @Inject constructor(
    private val planRepository: PlanRepository,
    private val entryRepository: EntryRepository
) : ViewModel() {

    private val today = LocalDate.now()

    private val _uiState = MutableStateFlow(GoalProgressState(today = today))
    val uiState: StateFlow<GoalProgressState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    fun onRetryClick() {
        observeData()
    }

    private fun observeData() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            combine(
                planRepository.observePlan(),
                entryRepository.observeAll()
            ) { planResult, entriesResult ->
                val planError = planResult.exceptionOrNull()
                val entriesError = entriesResult.exceptionOrNull()

                if (planError != null) {
                    GoalData(plan = null, transferSavingsTotal = 0L, error = planError.message ?: "Couldn't load plan.")
                } else if (entriesError != null) {
                    GoalData(plan = planResult.getOrNull(), transferSavingsTotal = 0L, error = entriesError.message ?: "Couldn't load entries.")
                } else {
                    val plan = planResult.getOrNull()
                    val entries = entriesResult.getOrNull().orEmpty()
                    val transferTotal = calculateSavingsTransfers(entries)
                    GoalData(plan = plan, transferSavingsTotal = transferTotal, error = null)
                }
            }.collect { data ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = data.error,
                        plan = data.plan,
                        transferSavingsTotal = data.transferSavingsTotal
                    )
                }
            }
        }
    }

    private fun calculateSavingsTransfers(entries: List<Entry>): Long =
        entries.filter { entry ->
            entry.type == EntryType.TRANSFER && !entry.isExpected &&
                (entry.toAccountId.contains("Savings", ignoreCase = true) ||
                    entry.account.contains("Savings", ignoreCase = true) ||
                    entry.category.contains("Savings", ignoreCase = true))
        }.sumOf { it.amount }

    private data class GoalData(
        val plan: lk.moneytrail.app.domain.model.MoneyPlan?,
        val transferSavingsTotal: Long,
        val error: String?
    )
}
