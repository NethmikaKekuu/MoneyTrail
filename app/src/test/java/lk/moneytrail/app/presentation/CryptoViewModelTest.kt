package lk.moneytrail.app.presentation

import android.content.ContextWrapper
import android.net.Uri
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import lk.moneytrail.app.domain.model.crypto.CryptoTrade
import lk.moneytrail.app.domain.model.crypto.CsvParseResult
import lk.moneytrail.app.domain.repository.CryptoRepository
import lk.moneytrail.app.domain.usecase.crypto.CalculateRealisedPnlUseCase
import lk.moneytrail.app.domain.usecase.crypto.ParseBinanceCsvUseCase
import lk.moneytrail.app.presentation.crypto.CryptoUiState
import lk.moneytrail.app.presentation.crypto.CryptoViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.LocalDate

class CryptoViewModelTest {

    private class FakeCryptoRepository : CryptoRepository {
        override suspend fun getExistingTrades(): List<CryptoTrade> = emptyList()
    }

    private class TestParseBinanceCsvUseCase(
        csvText: String,
        repository: CryptoRepository,
    ) : ParseBinanceCsvUseCase(object : ContextWrapper(null) {}, repository) {
        private val bytes = csvText.toByteArray(Charsets.UTF_8)

        override suspend fun invoke(uri: Uri): Result<CsvParseResult> {
            val stream: InputStream = ByteArrayInputStream(bytes)
            return parseInputStream(stream)
        }
    }

    @Test
    fun initialState_isEmpty() {
        val dummyContext = object : ContextWrapper(null) {}
        val repo = FakeCryptoRepository()
        val parseUseCase = ParseBinanceCsvUseCase(dummyContext, repo)
        val pnlUseCase = CalculateRealisedPnlUseCase()
        val viewModel = CryptoViewModel(parseUseCase, pnlUseCase)

        assertEquals(CryptoUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun importCsv_validContent_updatesStateToSuccess() = runBlocking {
        val currentYear = LocalDate.now().year
        val csv = """
            Date(UTC),Pair,Type,Price,Amount,Fee,Fee Coin,Trade ID
            $currentYear-05-10 12:00:00,BTCUSDT,BUY,30000.0,1.0,10.0,USDT,101
            $currentYear-05-15 14:00:00,BTCUSDT,SELL,40000.0,1.0,15.0,USDT,102
        """.trimIndent()

        val repo = FakeCryptoRepository()
        val parseUseCase = TestParseBinanceCsvUseCase(csv, repo)
        val pnlUseCase = CalculateRealisedPnlUseCase()
        val viewModel = CryptoViewModel(parseUseCase, pnlUseCase)

        viewModel.onImportCsvSelected(Uri.EMPTY)

        val state = viewModel.uiState.first { (it !is CryptoUiState.Loading) && (it !is CryptoUiState.Empty) }
        assertTrue(state is CryptoUiState.Success)
        val success = state as CryptoUiState.Success
        assertEquals(9975.0, success.ytdTotal, 1e-3)
    }

    @Test
    fun importCsv_missingHeader_updatesStateToError() = runBlocking {
        val invalidCsv = """
            Date(UTC),Type,Price,Amount
            2023-05-10 12:00:00,BUY,30000.0,1.0
        """.trimIndent()

        val repo = FakeCryptoRepository()
        val parseUseCase = TestParseBinanceCsvUseCase(invalidCsv, repo)
        val pnlUseCase = CalculateRealisedPnlUseCase()
        val viewModel = CryptoViewModel(parseUseCase, pnlUseCase)

        viewModel.onImportCsvSelected(Uri.EMPTY)

        val state = viewModel.uiState.first { (it !is CryptoUiState.Loading) && (it !is CryptoUiState.Empty) }
        assertTrue(state is CryptoUiState.Error)
        val error = state as CryptoUiState.Error
        assertTrue(error.message.contains("Missing required Binance CSV columns"))
    }
}
