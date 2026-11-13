package lk.moneytrail.app.presentation.usd

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.moneytrail.app.domain.model.Categories
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.UsdConversion
import lk.moneytrail.app.domain.repository.AccountRepository
import lk.moneytrail.app.domain.repository.UsdRateRepository
import lk.moneytrail.app.domain.usecase.AddEntryUseCase
import lk.moneytrail.app.presentation.navigation.Routes
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class UsdPayoutViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val usdRateRepository: UsdRateRepository,
    private val addEntryUseCase: AddEntryUseCase,
    accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UsdPayoutState(date = startDate(savedStateHandle)))
    val uiState: StateFlow<UsdPayoutState> = _uiState.asStateFlow()

    private var rateJob: Job? = null

    init {
        loadRate(_uiState.value.date)
        // Credited to the first account the user set up, as the screen says.
        viewModelScope.launch {
            accountRepository.observeAccounts().collect { result ->
                result.getOrNull()?.firstOrNull()?.let { account ->
                    _uiState.update { it.copy(accountName = account.name) }
                }
            }
        }
    }

    fun onUsdChange(text: String) {
        _uiState.update { it.copy(usdText = decimal(text), errorMessage = null) }
    }

    fun onLkrChange(text: String) {
        val digits = text.filter(Char::isDigit).trimStart('0').take(MAX_LKR_DIGITS)
        // Clearing the field hands the amount back to the rate.
        _uiState.update { it.copy(lkrOverride = digits.ifEmpty { null }, errorMessage = null) }
    }

    fun onDateChange(date: LocalDate) {
        if (date == _uiState.value.date) return
        _uiState.update { it.copy(date = date, errorMessage = null) }
        loadRate(date)
    }

    fun onTypeOwnRateClick() {
        _uiState.update {
            // Start from the suggestion so there is something to adjust.
            val start = (it.suggested as? SuggestedRate.Found)?.rate?.rate?.let(UsdConversion::formatHundredths).orEmpty()
            it.copy(isOwnRate = true, ownRateText = it.ownRateText.ifEmpty { start })
        }
    }

    fun onUseSuggestedClick() {
        _uiState.update { it.copy(isOwnRate = false) }
        if (_uiState.value.suggested is SuggestedRate.Failed) loadRate(_uiState.value.date)
    }

    fun onOwnRateChange(text: String) {
        _uiState.update { it.copy(ownRateText = decimal(text)) }
    }

    fun onRetryRateClick() = loadRate(_uiState.value.date)

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            addEntryUseCase(
                type = EntryType.INCOME,
                amount = state.lkr,
                category = Categories.ADSENSE,
                account = state.accountName,
                date = state.date,
                note = "",
                usdCents = state.usdCents,
                marketRate = state.marketRate
            )
                .onSuccess { _uiState.update { it.copy(isSaving = false, isSaved = true) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, errorMessage = e.message) } }
        }
    }

    private fun loadRate(date: LocalDate) {
        rateJob?.cancel()
        _uiState.update { it.copy(suggested = SuggestedRate.Loading) }
        rateJob = viewModelScope.launch {
            usdRateRepository.rateFor(date)
                .onSuccess { rate -> _uiState.update { it.copy(suggested = SuggestedRate.Found(rate)) } }
                .onFailure { e ->
                    _uiState.update { it.copy(suggested = SuggestedRate.Failed(e.message.orEmpty()), isOwnRate = true) }
                }
        }
    }

    private fun decimal(text: String) = text.filter { it.isDigit() || it == '.' }.take(MAX_DECIMAL_CHARS)

    private companion object {
        const val MAX_DECIMAL_CHARS = 12
        const val MAX_LKR_DIGITS = 9

        /** The day the new entry was started on, or today. */
        fun startDate(savedStateHandle: SavedStateHandle): LocalDate =
            savedStateHandle.get<String>(Routes.USD_PAYOUT_DATE_ARG)
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?: LocalDate.now()
    }
}
