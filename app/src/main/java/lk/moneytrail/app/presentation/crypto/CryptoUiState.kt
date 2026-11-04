package lk.moneytrail.app.presentation.crypto

import lk.moneytrail.app.domain.model.crypto.CryptoRealisedPnl
import java.time.YearMonth

/**
 * Represents the strict UI states for the Crypto module screen.
 */
sealed interface CryptoUiState {
    /**
     * CSV parsing or PnL computation is currently in progress.
     */
    data object Loading : CryptoUiState

    /**
     * No trades have been imported yet.
     */
    data object Empty : CryptoUiState

    /**
     * CSV trades were parsed and PnL was successfully calculated.
     *
     * @property monthlyRealisedPnl Realised PnL items grouped by month.
     * @property ytdTotal Sum of realised PnL for the current calendar year.
     */
    data class Success(
        val monthlyRealisedPnl: Map<YearMonth, List<CryptoRealisedPnl>>,
        val ytdTotal: Double,
    ) : CryptoUiState

    /**
     * An error occurred during CSV parsing or processing.
     *
     * @property message User-friendly error explanation.
     */
    data class Error(
        val message: String,
    ) : CryptoUiState
}
