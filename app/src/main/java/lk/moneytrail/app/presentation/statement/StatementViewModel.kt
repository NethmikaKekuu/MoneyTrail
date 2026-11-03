package lk.moneytrail.app.presentation.statement

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.moneytrail.app.domain.repository.AccountRepository
import lk.moneytrail.app.domain.repository.EntryRepository
import lk.moneytrail.app.domain.repository.FileExportRepository
import lk.moneytrail.app.domain.repository.MonthCloseRepository
import lk.moneytrail.app.presentation.navigation.Routes
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class StatementViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
    private val entryRepository: EntryRepository,
    private val monthCloseRepository: MonthCloseRepository,
    private val fileExport: FileExportRepository
) : ViewModel() {

    private val startMonth: YearMonth = savedStateHandle.get<String>(Routes.MONTH_ARG)
        ?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
        ?: YearMonth.now().minusMonths(1)

    private val _uiState = MutableStateFlow(StatementState(month = startMonth, latestMonth = YearMonth.now(), today = LocalDate.now()))
    val uiState: StateFlow<StatementState> = _uiState.asStateFlow()

    private var listenJob: Job? = null

    init {
        listen()
    }

    fun onRetryClick() = listen()

    fun onPreviousMonthClick() {
        _uiState.update { it.copy(month = it.month.minusMonths(1)) }
        listen()
    }

    fun onNextMonthClick() {
        if (!_uiState.value.canGoForward) return
        _uiState.update { it.copy(month = it.month.plusMonths(1)) }
        listen()
    }

    /** The user picked where to save the CSV; [uri] is null when they backed out. */
    fun onExportFileChosen(uri: String?) {
        if (uri == null) return
        val csv = _uiState.value.csv
        _uiState.update { it.copy(isExporting = true) }
        viewModelScope.launch {
            val result = fileExport.writeText(uri, csv)
            _uiState.update {
                it.copy(
                    isExporting = false,
                    message = if (result.isSuccess) {
                        "Statement saved as CSV"
                    } else {
                        result.exceptionOrNull()?.message ?: "Couldn't save the file."
                    }
                )
            }
        }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    /** Live: accounts, the entries for the opening balances, and this and last month's close. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun listen() {
        listenJob?.cancel()
        val month = _uiState.value.month
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        listenJob = viewModelScope.launch {
            accountRepository.observeAccounts().flatMapLatest { accountsResult ->
                val accounts = accountsResult.getOrElse { emptyList() }
                // From the first balance check, so opening balances can be worked out.
                val from = minOf(accounts.mapNotNull { it.checkedOn }.minOrNull() ?: month.atDay(1), month.atDay(1))
                combine(
                    entryRepository.observeBetween(from, month.atEndOfMonth()),
                    monthCloseRepository.observeClose(month),
                    monthCloseRepository.observeClose(month.minusMonths(1))
                ) { entries, close, previous ->
                    val error = accountsResult.exceptionOrNull() ?: entries.exceptionOrNull()
                    if (error != null) {
                        _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Couldn't load the statement.") }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = null,
                                accounts = accounts,
                                entries = entries.getOrDefault(emptyList()),
                                // A close that can't be read just leaves the "balanced" note out.
                                close = close.getOrNull(),
                                previousClose = previous.getOrNull()
                            )
                        }
                    }
                }
            }.collect {}
        }
    }
}
