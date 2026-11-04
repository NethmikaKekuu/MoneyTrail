package lk.moneytrail.app.presentation.crypto

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lk.moneytrail.app.domain.usecase.crypto.CalculateRealisedPnlUseCase
import lk.moneytrail.app.domain.usecase.crypto.ParseBinanceCsvUseCase
import java.time.LocalDate
import javax.inject.Inject

/**
 * ViewModel managing UI state and user actions for the Crypto trade import & PnL module.
 */
@HiltViewModel
class CryptoViewModel @Inject constructor(
    private val parseBinanceCsvUseCase: ParseBinanceCsvUseCase,
    private val calculateRealisedPnlUseCase: CalculateRealisedPnlUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CryptoUiState>(CryptoUiState.Empty)
    val uiState: StateFlow<CryptoUiState> = _uiState.asStateFlow()

    /**
     * Called when the user selects a Binance CSV file using the Storage Access Framework [Uri].
     * Emits [CryptoUiState.Loading], parses the CSV, calculates FIFO realised PnL & YTD total,
     * and updates [uiState] to [CryptoUiState.Success] or [CryptoUiState.Error].
     */
    fun onImportCsvSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = CryptoUiState.Loading

            val parseResult = parseBinanceCsvUseCase(uri)
            parseResult.fold(
                onSuccess = { result ->
                    val validTrades = result.read
                    if (validTrades.isEmpty()) {
                        _uiState.value = if (result.rejected.isNotEmpty()) {
                            CryptoUiState.Error(result.rejected.first().reason)
                        } else {
                            CryptoUiState.Empty
                        }
                    } else {
                        val monthlyPnlMap = calculateRealisedPnlUseCase(validTrades)
                        val currentYear = LocalDate.now().year
                        val ytdTotal = monthlyPnlMap.entries.asSequence()
                            .filter { (yearMonth, _) -> yearMonth.year == currentYear }
                            .flatMap { (_, pnlList) -> pnlList }
                            .sumOf { it.realisedPnl }

                        _uiState.value = CryptoUiState.Success(
                            monthlyRealisedPnl = monthlyPnlMap,
                            ytdTotal = ytdTotal,
                        )
                    }
                },
            ) { error ->
                _uiState.value = CryptoUiState.Error(
                    message = error.message ?: "Failed to import Binance CSV file.",
                )
            }
        }
    }
}
