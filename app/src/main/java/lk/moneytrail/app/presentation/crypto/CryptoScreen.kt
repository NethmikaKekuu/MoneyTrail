package lk.moneytrail.app.presentation.crypto

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.moneytrail.app.domain.model.crypto.CryptoMonth
import lk.moneytrail.app.domain.model.crypto.CryptoPairTotal
import lk.moneytrail.app.domain.model.crypto.CryptoSummary
import lk.moneytrail.app.presentation.components.EmptyView
import lk.moneytrail.app.presentation.components.ErrorView
import lk.moneytrail.app.presentation.components.LoadingView
import lk.moneytrail.app.presentation.freelance.FreelanceTopBar
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** File types a Binance export can arrive as, depending on the phone's file app. */
private val CsvTypes = arrayOf("text/csv", "text/comma-separated-values", "text/plain", "application/vnd.ms-excel", "application/octet-stream")

@Composable
fun CryptoScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CryptoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val pickCsv = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.onImportCsvSelected(uri)
    }
    val onImport = { pickCsv.launch(CsvTypes) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { FreelanceTopBar(title = "Crypto", onBackClick = onBackClick) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            val summary = uiState.summary
            when {
                uiState.isLoading -> LoadingView(message = "Loading your trades…")
                uiState.loadError != null -> ErrorView(message = uiState.loadError ?: "", onRetry = viewModel::onRetry)
                uiState.isImporting && uiState.isEmpty -> LoadingView(message = "Reading the CSV…")
                uiState.isEmpty || summary == null -> EmptyView(
                    title = "No crypto trades yet",
                    message = "On Binance: Orders → Trade History → Export. Then import the CSV here to see what selling made or lost. Crypto stays out of your income and plan.",
                    actionLabel = "Import Binance CSV",
                    onAction = onImport
                )
                else -> CryptoContent(
                    summary = summary,
                    lastImport = uiState.lastImport,
                    isImporting = uiState.isImporting,
                    onImport = onImport,
                    onRemoveAll = viewModel::onAskDeleteAll
                )
            }
        }
    }

    if (uiState.isDeleteConfirmOpen) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissDeleteAll,
            title = { Text("Remove all imported trades?", fontWeight = FontWeight.Bold) },
            text = { Text("Every trade from your CSV imports is removed. You can import the file again any time.") },
            confirmButton = {
                TextButton(onClick = viewModel::onConfirmDeleteAll) {
                    Text("Remove", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = viewModel::onDismissDeleteAll) { Text("Cancel") } }
        )
    }
}

@Composable
private fun CryptoContent(
    summary: CryptoSummary,
    lastImport: ImportResult?,
    isImporting: Boolean,
    onImport: () -> Unit,
    onRemoveAll: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { YearCard(summary) }
        item {
            Button(
                onClick = onImport,
                enabled = !isImporting,
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (isImporting) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Import Binance CSV", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
        }
        lastImport?.let { result -> item { ImportCard(result) } }

        if (summary.pairs.isNotEmpty()) {
            item { SectionTitle("By coin, ${summary.year}") }
            item {
                OutlinedCard {
                    summary.pairs.forEachIndexed { i, pair ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        PairRow(pair)
                    }
                }
            }
        }

        item { SectionTitle("By month") }
        if (summary.months.isEmpty()) {
            item {
                Text(
                    "No sells yet. Profit or loss is realised when you sell.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(summary.months, key = { it.month.toString() }) { month -> MonthCard(month) }

        item {
            TextButton(onClick = onRemoveAll, modifier = Modifier.fillMaxWidth()) {
                Text("Remove all imported trades", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun pnlColor(value: Double): Color = when {
    value > 0.005 -> MaterialTheme.colorScheme.primary
    value < -0.005 -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurface
}

@Composable
private fun YearCard(summary: CryptoSummary) {
    OutlinedCard {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Realised in ${summary.year}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(formatPnl(summary.yearTotal), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = pnlColor(summary.yearTotal))
                Text("USDT", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            }
            Text(
                "All time ${formatPnl(summary.allTimeTotal)} · ${summary.tradeCount} trades. Worked out first-in, first-out after fees. Not counted as income or in your plan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ImportCard(result: ImportResult) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Last import: ${result.summary}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            result.skippedRows.forEach {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MonthCard(month: CryptoMonth) {
    OutlinedCard {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(month.month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "${month.sells} sell${if (month.sells == 1) "" else "s"} · fees ${String.format(Locale.US, "%,.2f", month.fees)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(formatPnl(month.realisedPnl), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = pnlColor(month.realisedPnl))
        }
    }
}

@Composable
private fun PairRow(pair: CryptoPairTotal) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(pair.pair, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(formatPnl(pair.realisedPnl), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = pnlColor(pair.realisedPnl))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun OutlinedCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface
    ) { Column { content() } }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun CryptoContentPreview() {
    MoneyTrailTheme {
        CryptoContent(
            summary = CryptoSummary(
                year = 2026,
                yearTotal = 412.35,
                allTimeTotal = 160.10,
                months = listOf(
                    CryptoMonth(YearMonth.of(2026, 10), 520.10, 12.4, 3),
                    CryptoMonth(YearMonth.of(2026, 8), -107.75, 4.1, 1)
                ),
                pairs = listOf(CryptoPairTotal("BTCUSDT", 520.10), CryptoPairTotal("ETHUSDT", -107.75)),
                tradeCount = 18
            ),
            lastImport = ImportResult("12 trades added · 3 already imported · 1 row skipped", listOf("Row 5: Invalid price")),
            isImporting = false,
            onImport = {},
            onRemoveAll = {}
        )
    }
}
