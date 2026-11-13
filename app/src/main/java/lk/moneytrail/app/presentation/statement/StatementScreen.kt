package lk.moneytrail.app.presentation.statement

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.moneytrail.app.domain.model.Account
import lk.moneytrail.app.domain.model.AccountKind
import lk.moneytrail.app.domain.model.AccountStatement
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.MonthClose
import lk.moneytrail.app.domain.model.SpendingType
import lk.moneytrail.app.presentation.auth.AuthBackButton
import lk.moneytrail.app.presentation.components.EmptyView
import lk.moneytrail.app.presentation.components.ErrorView
import lk.moneytrail.app.presentation.components.LoadingView
import lk.moneytrail.app.presentation.components.MoneyTrailIcons
import lk.moneytrail.app.presentation.components.formatAmount
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

// MoneyTrail-Figma-HTML/22-Statement.html
@Composable
fun StatementScreen(
    onBackClick: () -> Unit,
    onCloseMonthClick: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // The system "Save to" picker: the user chooses where the CSV goes.
    val saveCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        viewModel.onExportFileChosen(uri?.toString())
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    Box(modifier.fillMaxSize()) {
        StatementContent(
            uiState = uiState,
            onBackClick = onBackClick,
            onRetryClick = viewModel::onRetryClick,
            onPreviousMonthClick = viewModel::onPreviousMonthClick,
            onNextMonthClick = viewModel::onNextMonthClick,
            onExportClick = { saveCsv.launch(uiState.fileName) },
            onCloseMonthClick = { onCloseMonthClick(uiState.month) }
        )
        SnackbarHost(
            snackbarHostState,
            Modifier
                .align(Alignment.BottomCenter)
                .systemBarsPadding()
        )
    }
}

private val MonthYear = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
private val DayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

@Composable
private fun StatementContent(
    uiState: StatementState,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onPreviousMonthClick: () -> Unit,
    onNextMonthClick: () -> Unit,
    onExportClick: () -> Unit,
    onCloseMonthClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AuthBackButton(onClick = onBackClick, contentDescription = "Back")
            Text(
                text = "Statement",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.size(44.dp))
        }

        // Pick the month: back as far as wanted, forward up to this one.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonthClick) {
                Icon(MoneyTrailIcons.ChevronLeft, contentDescription = "Previous month")
            }
            Text(
                text = uiState.month.format(MonthYear),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onNextMonthClick, enabled = uiState.canGoForward) {
                Icon(MoneyTrailIcons.ChevronRight, contentDescription = "Next month")
            }
        }

        when {
            uiState.isLoading -> LoadingView(message = "Loading the statement…")
            uiState.errorMessage != null -> ErrorView(
                title = "Couldn't load the statement",
                message = uiState.errorMessage,
                onRetry = onRetryClick
            )
            uiState.isEmpty -> EmptyView(
                title = "Nothing in ${uiState.month.format(MonthYear)}",
                message = "Entries you confirm on the calendar for this month will show here."
            )
            else -> StatementBody(uiState, onExportClick, onCloseMonthClick)
        }
    }
}

@Composable
private fun StatementBody(uiState: StatementState, onExportClick: () -> Unit, onCloseMonthClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val totals = uiState.totals
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ClosedBanner(uiState.close, onCloseMonthClick)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.outlineVariant, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TotalRow("Income", totals.income)
            TotalRow("Expenses", totals.expenses)
            if (totals.unaccounted != 0L) TotalRow("Unaccounted", totals.unaccounted)
            TotalRow("Kept", totals.kept, bold = true)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("By account · opening + in − out = closing", style = MaterialTheme.typography.labelLarge)
            uiState.lines.forEach { AccountLine(it) }
        }

        Text(
            text = "The CSV has every confirmed entry of the month in LKR, expenses as negative amounts, so a SUM in a spreadsheet gives what you kept.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.outline
        )
        Button(
            onClick = onExportClick,
            enabled = !uiState.isExporting,
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (uiState.isExporting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.onPrimary)
            } else {
                Text("Export CSV", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** "Books balanced" once the month is closed, else a way to close it. */
@Composable
private fun ClosedBanner(close: MonthClose?, onCloseMonthClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    if (close != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.primaryContainer, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(MoneyTrailIcons.Check, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(20.dp))
            Column {
                Text("Books balanced", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = colors.onPrimaryContainer)
                Text(
                    "Closed ${close.closedOn.format(DayMonth)} · every account checked",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onPrimaryContainer
                )
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surfaceContainerLow, RoundedCornerShape(16.dp))
                .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Not closed yet: check each account against the bank.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(onClick = onCloseMonthClick) { Text("Close month") }
        }
    }
}

@Composable
private fun TotalRow(label: String, amount: Long, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        Text(
            formatAmount(amount),
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}

@Composable
private fun AccountLine(line: AccountStatement) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(line.account.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(line.closing?.let { formatAmount(it) } ?: "—", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        val opening = line.opening?.let { formatAmount(it) } ?: "?"
        Text(
            "$opening + ${formatAmount(line.moneyIn)} − ${formatAmount(line.moneyOut)}",
            style = MaterialTheme.typography.labelSmall,
            color = if (line.opening == null) colors.outline else colors.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun StatementScreenPreview() {
    val month = YearMonth.of(2026, 9)
    val main = Account("main-bank", "Main bank", "", AccountKind.BANK, 41_520, month.atDay(1))
    MoneyTrailTheme {
        StatementContent(
            uiState = StatementState(
                month = month,
                latestMonth = YearMonth.of(2026, 10),
                isLoading = false,
                accounts = listOf(main),
                entries = listOf(
                    Entry("1", EntryType.INCOME, 181_000, "Salary", "Main bank", month.atDay(25), "", null),
                    Entry("2", EntryType.EXPENSE, 154_800, "Rent", "Main bank", month.atDay(1), "", SpendingType.COMMITTED)
                ),
                close = MonthClose(month, mapOf("main-bank" to 67_720), LocalDate.of(2026, 10, 1))
            ),
            onBackClick = {},
            onRetryClick = {},
            onPreviousMonthClick = {},
            onNextMonthClick = {},
            onExportClick = {},
            onCloseMonthClick = {}
        )
    }
}
