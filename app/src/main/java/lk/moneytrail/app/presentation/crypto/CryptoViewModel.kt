package lk.moneytrail.app.presentation.crypto

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.moneytrail.app.domain.model.crypto.cryptoSummary
import lk.moneytrail.app.domain.model.crypto.summaryText
import lk.moneytrail.app.domain.repository.CryptoRepository
import lk.moneytrail.app.domain.usecase.crypto.CalculateRealisedPnlUseCase
import lk.moneytrail.app.domain.usecase.crypto.ImportCryptoTradesUseCase
import java.time.LocalDate
import javax.inject.Inject

/**
 * Crypto: import a Binance trade history CSV, save the trades, and show the realised
 * profit or loss (FIFO, after fees) by month. Kept apart from income and the plan.
 */
@HiltViewModel
class CryptoViewModel @Inject constructor(
    private val importCryptoTrades: ImportCryptoTradesUseCase,
    private val calculateRealisedPnlUseCase: CalculateRealisedPnlUseCase,
    private val cryptoRepository: CryptoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CryptoUiState())
    val uiState: StateFlow<CryptoUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        observeTrades()
    }

    fun onRetry() = observeTrades()

    private fun observeTrades() {
        observeJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        observeJob = viewModelScope.launch {
            cryptoRepository.observeTrades().collect { result ->
                result
                    .onSuccess { trades ->
                        val monthly = calculateRealisedPnlUseCase(trades)
                        _uiState.update {
                            it.copy(isLoading = false, loadError = null, summary = cryptoSummary(trades, monthly, LocalDate.now().year))
                        }
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(isLoading = false, loadError = error.message ?: "Couldn't load your crypto trades.") }
                    }
            }
        }
    }

    /** The user picked a Binance CSV: read it, save the new trades, and say what happened. */
    fun onImportCsvSelected(uri: Uri) {
        if (_uiState.value.isImporting) return
        _uiState.update { it.copy(isImporting = true, message = null) }
        viewModelScope.launch {
            importCryptoTrades(uri)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            lastImport = ImportResult(result.summaryText(), result.rejected.take(MAX_SKIPPED_SHOWN).map { r -> r.describe() })
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isImporting = false, message = error.message ?: "Couldn't import that file.") }
                }
        }
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    fun onAskDeleteAll() = _uiState.update { it.copy(isDeleteConfirmOpen = true) }
    fun onDismissDeleteAll() = _uiState.update { it.copy(isDeleteConfirmOpen = false) }

    fun onConfirmDeleteAll() {
        _uiState.update { it.copy(isDeleteConfirmOpen = false) }
        viewModelScope.launch {
            cryptoRepository.deleteAllTrades()
                .onSuccess { _uiState.update { it.copy(lastImport = null, message = "Imported trades removed.") } }
                .onFailure { e -> _uiState.update { it.copy(message = e.message ?: "Couldn't remove the imported trades.") } }
        }
    }

    private companion object {
        const val MAX_SKIPPED_SHOWN = 3
    }
}
