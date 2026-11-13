package lk.moneytrail.app.presentation.monthlyclose

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.moneytrail.app.domain.repository.AccountRepository
import lk.moneytrail.app.domain.usecase.CloseMonthUseCase
import lk.moneytrail.app.presentation.navigation.Routes
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class MonthlyCloseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
    private val closeMonth: CloseMonthUseCase
) : ViewModel() {

    /** The month from the route, else last month: the latest one that has ended. */
    private val month: YearMonth = savedStateHandle.get<String>(Routes.MONTH_ARG)
        ?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
        ?: YearMonth.now().minusMonths(1)

    private val _uiState = MutableStateFlow(MonthlyCloseState(month = month))
    val uiState: StateFlow<MonthlyCloseState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onRetryClick() = load()

    fun onRealBalanceChange(accountId: String, text: String) {
        val digits = text.filter(Char::isDigit).take(12)
        _uiState.update { state ->
            state.copy(
                rows = state.rows.map { if (it.account.id == accountId) it.copy(realDigits = digits) else it },
                saveError = null
            )
        }
    }

    fun onCloseClick() {
        val state = _uiState.value
        if (!state.canClose) return
        _uiState.update { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            val balances = state.rows.mapNotNull { row -> row.real?.let { row.account.id to it } }.toMap()
            closeMonth(month, state.rows.map { it.account }, balances, LocalDate.now())
                .onSuccess { _uiState.update { it.copy(isSaving = false, isClosed = true) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, saveError = e.message ?: "Couldn't close the month.") } }
        }
    }

    private fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val accounts = accountRepository.observeAccounts().first().getOrElse { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Couldn't load your accounts.") }
                return@launch
            }
            val calculated = closeMonth.calculated(month, accounts).getOrElse { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Couldn't work out your balances.") }
                return@launch
            }
            _uiState.update {
                it.copy(isLoading = false, rows = accounts.map { account -> CloseRow(account, calculated[account.id]) })
            }
        }
    }
}
