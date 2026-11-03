package lk.moneytrail.app.presentation.monthlyclose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.moneytrail.app.domain.model.Account
import lk.moneytrail.app.domain.model.AccountKind
import lk.moneytrail.app.presentation.auth.AuthBackButton
import lk.moneytrail.app.presentation.components.AmountVisualTransformation
import lk.moneytrail.app.presentation.components.EmptyView
import lk.moneytrail.app.presentation.components.ErrorView
import lk.moneytrail.app.presentation.components.LoadingView
import lk.moneytrail.app.presentation.components.formatAmount
import lk.moneytrail.app.presentation.components.formatNet
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

// MoneyTrail-Figma-HTML/21-MonthlyClose.html
@Composable
fun MonthlyCloseScreen(
    onBackClick: () -> Unit,
    /** The month is closed: show its statement. */
    onClosed: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MonthlyCloseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isClosed) {
        if (uiState.isClosed) onClosed(uiState.month)
    }

    MonthlyCloseContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onRetryClick = viewModel::onRetryClick,
        onRealBalanceChange = viewModel::onRealBalanceChange,
        onCloseClick = viewModel::onCloseClick,
        modifier = modifier
    )
}

private val MonthName = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)
private val DayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

@Composable
private fun MonthlyCloseContent(
    uiState: MonthlyCloseState,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onRealBalanceChange: (accountId: String, text: String) -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AuthBackButton(onClick = onBackClick, contentDescription = "Back")
            Text(
                text = "Close ${uiState.month.format(MonthName)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.size(44.dp))
        }

        when {
            uiState.isLoading -> LoadingView(message = "Working out your balances…")
            uiState.errorMessage != null -> ErrorView(
                title = "Couldn't load the close",
                message = uiState.errorMessage,
                onRetry = onRetryClick
            )
            uiState.rows.isEmpty() -> EmptyView(
                title = "No accounts yet",
                message = "Finish onboarding to add the places you keep money, then close the month here."
            )
            else -> CloseForm(uiState, onRealBalanceChange, onCloseClick)
        }
    }
}

@Composable
private fun CloseForm(
    uiState: MonthlyCloseState,
    onRealBalanceChange: (accountId: String, text: String) -> Unit,
    onCloseClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val lastDay = uiState.month.atEndOfMonth().format(DayMonth)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Open your banking apps and type each real balance on $lastDay. We'll compare it with what MoneyTrail worked out.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant
        )

        uiState.rows.forEach { row ->
            CloseAccountCard(row = row, enabled = !uiState.isSaving, onChange = { onRealBalanceChange(row.account.id, it) })
        }

        val off = uiState.offCount
        Text(
            text = when {
                uiState.rows.any { it.real == null } -> "Type every account's balance to close the month."
                off == 0 -> "Every account matches."
                off == 1 -> "1 account is off: the difference is saved as Not logged on $lastDay."
                else -> "$off accounts are off: each difference is saved as Not logged on $lastDay."
            },
            style = MaterialTheme.typography.bodySmall,
            color = colors.outline
        )
        uiState.saveError?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = colors.error)
        }
        Button(
            onClick = onCloseClick,
            enabled = uiState.canClose,
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.onPrimary)
            } else {
                Text("Close ${uiState.month.format(MonthName)}", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun CloseAccountCard(row: CloseRow, enabled: Boolean, onChange: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val difference = row.difference
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(row.account.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            val (status, statusColor) = when {
                row.calculated == null -> "First check" to colors.outline
                difference == null -> "" to colors.outline
                difference == 0L -> "Matches" to colors.primary
                else -> "Off by ${formatNet(difference)}" to colors.error
            }
            if (status.isNotEmpty()) {
                Text(status, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = statusColor)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Calculated", style = MaterialTheme.typography.labelSmall, color = colors.outline)
                Text(
                    text = row.calculated?.let { formatAmount(it) } ?: "—",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            OutlinedTextField(
                // Plain digits, shown with commas, so the cursor stays where the user types.
                value = row.realDigits,
                onValueChange = onChange,
                visualTransformation = AmountVisualTransformation,
                label = { Text("Real balance") },
                singleLine = true,
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1.4f)
            )
        }
        if (row.calculated == null) {
            Text(
                "Not balanced before this month, so this becomes its starting point.",
                style = MaterialTheme.typography.labelSmall,
                color = colors.outline
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun MonthlyCloseScreenPreview() {
    MoneyTrailTheme {
        MonthlyCloseContent(
            uiState = MonthlyCloseState(
                month = YearMonth.of(2026, 9),
                isLoading = false,
                rows = listOf(
                    CloseRow(Account("main-bank", "Main bank", "", AccountKind.BANK), 48_320, "46900"),
                    CloseRow(Account("secondary-bank", "Secondary bank", "", AccountKind.BANK), 8_150, "8150"),
                    CloseRow(Account("savings", "Savings", "", AccountKind.SAVINGS), null)
                )
            ),
            onBackClick = {},
            onRetryClick = {},
            onRealBalanceChange = { _, _ -> },
            onCloseClick = {}
        )
    }
}
