package lk.moneytrail.app.presentation.catchup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.moneytrail.app.domain.model.Account
import lk.moneytrail.app.domain.model.AccountKind
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryStatus
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.SpendingType
import lk.moneytrail.app.presentation.components.AmountVisualTransformation
import lk.moneytrail.app.presentation.components.LoadingView
import lk.moneytrail.app.presentation.components.MoneyTrailIcons
import lk.moneytrail.app.presentation.components.formatAmount
import lk.moneytrail.app.presentation.components.formatNet
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// MoneyTrail-Figma-HTML/23-CatchUp.html, with the balance step from 21-MonthlyClose.html.
@Composable
fun CatchUpScreen(
    onAddEntryClick: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CatchUpViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) onFinished()
    }

    CatchUpContent(
        uiState = uiState,
        actions = CatchUpActions(
            onLater = viewModel::onLaterClick,
            onEntryToggle = viewModel::onEntryToggle,
            onConfirmEntries = viewModel::onConfirmEntriesClick,
            onSkipConfirm = viewModel::onSkipConfirmClick,
            onAddEntry = onAddEntryClick,
            onLogNext = viewModel::onLogNextClick,
            onRealBalanceChange = viewModel::onRealBalanceChange,
            onBalance = viewModel::onBalanceClick,
            onSkipBalance = viewModel::onSkipBalanceClick,
            onDone = viewModel::onDoneClick
        ),
        modifier = modifier
    )
}

/** Everything the catch-up steps can do, so the content takes one parameter for them. */
data class CatchUpActions(
    val onLater: () -> Unit,
    val onEntryToggle: (String) -> Unit,
    val onConfirmEntries: () -> Unit,
    val onSkipConfirm: () -> Unit,
    val onAddEntry: () -> Unit,
    val onLogNext: () -> Unit,
    val onRealBalanceChange: (accountId: String, text: String) -> Unit,
    val onBalance: () -> Unit,
    val onSkipBalance: () -> Unit,
    val onDone: () -> Unit
)

private val DayFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

@Composable
private fun CatchUpContent(
    uiState: CatchUpState,
    actions: CatchUpActions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = uiState.stepNumber?.let { "Catch-up · step $it of 3" } ?: "Catch-up",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.weight(1f)
            )
            if (uiState.step != CatchUpStep.DONE) {
                TextButton(onClick = actions.onLater) { Text("Later") }
            }
        }

        if (uiState.isLoading) {
            LoadingView(message = "Getting you up to date…")
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (uiState.step) {
                CatchUpStep.CONFIRM -> ConfirmStep(uiState, actions)
                CatchUpStep.LOG -> LogStep(uiState, actions)
                CatchUpStep.BALANCE -> BalanceStep(uiState, actions)
                CatchUpStep.DONE -> DoneStep(uiState, actions)
            }

            uiState.errorMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun StepTitle(title: String, message: String) {
    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** "It's been 9 days." for someone back after a break; nothing when opened from More. */
private fun awayText(daysAway: Long?): String =
    daysAway?.takeIf { it >= 2 }?.let { "It's been $it days. No problem: let's get you current in about 2 minutes. " }.orEmpty()

@Composable
private fun ConfirmStep(uiState: CatchUpState, actions: CatchUpActions) {
    StepTitle(
        title = "Welcome back!",
        message = awayText(uiState.daysAway) + "First, did these happen?"
    )
    uiState.dueEntries.forEach { entry ->
        DueRow(entry = entry, isTicked = entry.id !in uiState.untickedIds, onToggle = { actions.onEntryToggle(entry.id) })
    }
    PrimaryButton(
        text = "Confirm ${uiState.tickedCount} ${if (uiState.tickedCount == 1) "item" else "items"}",
        enabled = uiState.tickedCount > 0,
        isLoading = uiState.isSaving,
        onClick = actions.onConfirmEntries
    )
    TextButton(onClick = actions.onSkipConfirm, modifier = Modifier.fillMaxWidth()) { Text("None of these yet") }
}

@Composable
private fun DueRow(entry: Entry, isTicked: Boolean, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .background(colors.surfaceContainerLow, RoundedCornerShape(16.dp))
            .toggleable(value = isTicked, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = isTicked, onCheckedChange = null, modifier = Modifier.padding(horizontal = 8.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(entry.category, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = "${entry.date.format(DayFormatter)} · ${entry.account}",
                style = MaterialTheme.typography.labelSmall,
                color = colors.outline
            )
        }
        Text(
            text = (if (entry.type == EntryType.INCOME) "+" else "") + formatAmount(entry.amount),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (entry.type == EntryType.INCOME) colors.secondary else colors.onSurface,
            modifier = Modifier.padding(end = 8.dp)
        )
    }
}

@Composable
private fun LogStep(uiState: CatchUpState, actions: CatchUpActions) {
    StepTitle(
        title = if (uiState.dueEntries.isEmpty()) "Welcome back!" else "Anything else you remember?",
        message = (if (uiState.dueEntries.isEmpty()) awayText(uiState.daysAway) else "") +
            "Coffee, rides, food delivery… log what you remember. Don't worry about the rest: " +
            "the next step finds it when you balance your accounts."
    )
    OutlinedButton(
        onClick = actions.onAddEntry,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Icon(MoneyTrailIcons.Plus, contentDescription = null, modifier = Modifier.size(18.dp))
        Text("Add an entry", modifier = Modifier.padding(start = 8.dp))
    }
    PrimaryButton(
        text = if (uiState.balanceRows.isEmpty()) "Finish" else "Next: balance accounts",
        enabled = true,
        isLoading = false,
        onClick = if (uiState.balanceRows.isEmpty()) actions.onSkipBalance else actions.onLogNext
    )
}

@Composable
private fun BalanceStep(uiState: CatchUpState, actions: CatchUpActions) {
    StepTitle(
        title = "Balance your accounts",
        message = "Open your banking apps and type what each account really holds. Anything you didn't log is " +
            "added as \"Not logged\", so MoneyTrail matches your bank again."
    )
    uiState.balanceRows.forEach { row ->
        BalanceCard(row = row, enabled = !uiState.isSaving, onChange = { actions.onRealBalanceChange(row.account.id, it) })
    }
    PrimaryButton(
        text = "Balance accounts",
        enabled = uiState.balanceRows.any { it.real != null },
        isLoading = uiState.isSaving,
        onClick = actions.onBalance
    )
    TextButton(onClick = actions.onSkipBalance, modifier = Modifier.fillMaxWidth()) { Text("Skip for now") }
}

@Composable
private fun BalanceCard(row: BalanceRow, enabled: Boolean, onChange: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(16.dp), color = colors.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(row.account.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    text = row.calculated?.let { "MoneyTrail: LKR ${formatAmount(it)}" } ?: "First time",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.outline
                )
            }
            OutlinedTextField(
                // Plain digits, shown with commas, so the cursor stays where the user types.
                value = row.realDigits,
                onValueChange = onChange,
                visualTransformation = AmountVisualTransformation,
                label = { Text("Real balance (LKR)") },
                singleLine = true,
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            val difference = row.difference
            val note = when {
                row.calculated == null -> "This becomes the starting point; next time it's checked against it."
                difference == null -> null
                difference == 0L -> "Matches"
                difference < 0 -> "Off by ${formatNet(difference)}: spent but not logged"
                else -> "Off by ${formatNet(difference)}: received but not logged"
            }
            note?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (difference == 0L) colors.primary else colors.outline
                )
            }
        }
    }
}

@Composable
private fun DoneStep(uiState: CatchUpState, actions: CatchUpActions) {
    Spacer(Modifier.height(24.dp))
    Icon(
        imageVector = MoneyTrailIcons.Check,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(40.dp)
    )
    val found = buildList {
        if (uiState.notLoggedSpent > 0) {
            add("LKR ${formatAmount(uiState.notLoggedSpent)} you spent without logging is now in your folders as \"Not logged\".")
        }
        if (uiState.notLoggedReceived > 0) {
            add("LKR ${formatAmount(uiState.notLoggedReceived)} you received without logging was added too.")
        }
        if (isEmpty() && uiState.balancedCount > 0) add("Every account matches your bank.")
        add("Log a little each day, and your goal and insights stay right.")
    }
    StepTitle(title = "You're back on track", message = found.joinToString("\n\n"))
    PrimaryButton(text = "Done", enabled = true, isLoading = false, onClick = actions.onDone)
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean, isLoading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private val PreviewActions = CatchUpActions({}, {}, {}, {}, {}, {}, { _, _ -> }, {}, {}, {})

// Sample entries for the previews only, from the assignment scenario.
@Preview(showBackground = true, heightDp = 700)
@Composable
private fun CatchUpConfirmPreview() {
    MoneyTrailTheme {
        CatchUpContent(
            uiState = CatchUpState(
                isLoading = false,
                daysAway = 9,
                dueEntries = listOf(
                    Entry("1", EntryType.EXPENSE, 34_000, "Rent", "Main bank", LocalDate.of(2026, 10, 1), "", SpendingType.COMMITTED, EntryStatus.EXPECTED),
                    Entry("2", EntryType.EXPENSE, 6_500, "Gym", "Secondary bank", LocalDate.of(2026, 10, 9), "", SpendingType.COMMITTED, EntryStatus.EXPECTED)
                )
            ),
            actions = PreviewActions
        )
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun CatchUpBalancePreview() {
    MoneyTrailTheme {
        CatchUpContent(
            uiState = CatchUpState(
                isLoading = false,
                step = CatchUpStep.BALANCE,
                balanceRows = listOf(
                    BalanceRow(Account("main-bank", "Main bank", "", AccountKind.BANK), calculated = 48_320, realDigits = "46900"),
                    BalanceRow(Account("cash-wallet", "Cash wallet", "", AccountKind.CASH), calculated = null)
                )
            ),
            actions = PreviewActions
        )
    }
}
