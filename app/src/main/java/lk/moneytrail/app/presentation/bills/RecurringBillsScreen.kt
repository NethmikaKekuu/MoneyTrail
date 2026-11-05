package lk.moneytrail.app.presentation.bills

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.moneytrail.app.domain.model.RecurringBill
import lk.moneytrail.app.presentation.components.ChipEmphasis
import lk.moneytrail.app.presentation.components.EmptyView
import lk.moneytrail.app.presentation.components.ErrorView
import lk.moneytrail.app.presentation.components.LoadingView
import lk.moneytrail.app.presentation.components.MoneyTrailIcons
import lk.moneytrail.app.presentation.components.ScreenTopBar
import lk.moneytrail.app.presentation.components.StatusChip
import lk.moneytrail.app.presentation.components.formatAmount
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme
import java.time.LocalDate

/**
 * Recurring bills: the subscriptions and auto-debits that leave an account on the same
 * day every month. Each month an active bill appears on the calendar to be checked with
 * "Did you pay it?". Laid out like Freelance: totals card, then one card per bill.
 */
@Composable
fun RecurringBillsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecurringBillsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val message = uiState.message
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.onMessageShown()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            ScreenTopBar(
                title = "Recurring bills",
                onBackClick = onBackClick,
                actionIcon = MoneyTrailIcons.Plus,
                actionDescription = "Add a bill",
                onActionClick = viewModel::onAddClick
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                uiState.isLoading -> LoadingView(message = "Loading your bills…")
                uiState.loadError != null -> ErrorView(message = uiState.loadError ?: "", onRetry = viewModel::onRetryClick)
                uiState.bills.isEmpty() -> EmptyView(
                    title = "No recurring bills yet",
                    message = "Add your subscriptions and auto-debits once. Each month they appear on the calendar, ready to mark as paid.",
                    actionLabel = "Add a bill",
                    onAction = viewModel::onAddClick
                )
                else -> BillsList(
                    state = uiState,
                    today = LocalDate.now(),
                    onEditClick = viewModel::onEditClick,
                    onActiveChange = viewModel::onActiveChange
                )
            }
        }
    }

    uiState.draft?.let { draft ->
        BillEditorDialog(
            draft = draft,
            accountNames = uiState.accountNames,
            categoryNames = uiState.categoryNames,
            isSaving = uiState.isSaving,
            onNameChange = viewModel::onNameChange,
            onAmountChange = viewModel::onAmountChange,
            onDueDayChange = viewModel::onDueDayChange,
            onAccountSelect = viewModel::onAccountSelect,
            onCategorySelect = viewModel::onCategorySelect,
            onSave = viewModel::onSaveClick,
            onDelete = viewModel::onDeleteFromDraft,
            onDismiss = viewModel::onDraftDismiss
        )
    }

    uiState.deleting?.let { bill ->
        AlertDialog(
            onDismissRequest = viewModel::onDeleteDismiss,
            title = { Text("Delete ${bill.name}?", fontWeight = FontWeight.Bold) },
            text = { Text("It will stop appearing on the calendar. Entries it already made stay.") },
            confirmButton = {
                TextButton(onClick = viewModel::onDeleteConfirm) {
                    Text("Delete", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = viewModel::onDeleteDismiss) { Text("Cancel") } }
        )
    }
}

@Composable
private fun BillsList(
    state: RecurringBillsState,
    today: LocalDate,
    onEditClick: (RecurringBill) -> Unit,
    onActiveChange: (RecurringBill, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { TotalsCard(state, today) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Bills", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${state.bills.size}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(state.sortedBills, key = { it.id }) { bill ->
            BillCard(
                bill = bill,
                onEditClick = { onEditClick(bill) },
                onActiveChange = { onActiveChange(bill, it) }
            )
        }
        item {
            Text(
                "Tap a bill to edit or delete it. Switch it off to stop it without losing its history.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

/** Same layout as the Freelance totals: the main figure, a divider, three small figures. */
@Composable
private fun TotalsCard(state: RecurringBillsState, today: LocalDate) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Bills every month (active)", style = MaterialTheme.typography.bodyMedium, color = onPrimary.copy(alpha = 0.85f))
                Text(
                    "LKR ${formatAmount(state.monthlyTotal)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = onPrimary
                )
            }
            HorizontalDivider(color = onPrimary.copy(alpha = 0.2f))
            Row(Modifier.fillMaxWidth()) {
                TotalStat("Active", "${state.activeCount}", "bills", Modifier.weight(1f))
                TotalStat("Stopped", "${state.stoppedCount}", "bills", Modifier.weight(1f))
                TotalStat("Next due", state.nextDue(today)?.second?.dayOfMonth?.toString() ?: "–", state.nextDue(today)?.first?.name ?: "none", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TotalStat(label: String, value: String, caption: String, modifier: Modifier) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = onPrimary.copy(alpha = 0.8f))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = onPrimary, maxLines = 1)
        Text(caption, style = MaterialTheme.typography.bodySmall, color = onPrimary.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Same card as a Freelance project: name and details on the left, amount and chip on the right. */
@Composable
private fun BillCard(
    bill: RecurringBill,
    onEditClick: () -> Unit,
    onActiveChange: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onEditClick,
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(18.dp))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        bill.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (bill.isActive) colors.onSurface else colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${bill.category} · ${bill.account}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(formatAmount(bill.amount), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    if (bill.isActive) StatusChip("Active", ChipEmphasis.ACTIVE) else StatusChip("Stopped", ChipEmphasis.QUIET)
                }
            }
            HorizontalDivider(color = colors.outlineVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    bill.dueDayText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = bill.isActive, onCheckedChange = onActiveChange)
            }
        }
    }
}

/** Same dialog look as the milestone dialog: bold title, rounded outlined fields. */
@Composable
private fun BillEditorDialog(
    draft: BillDraft,
    accountNames: List<String>,
    categoryNames: List<String>,
    isSaving: Boolean,
    onNameChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onDueDayChange: (String) -> Unit,
    onAccountSelect: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val enabled = !isSaving
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (draft.isNew) "Add bill" else "Edit bill",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DialogField(draft.name, onNameChange, "Name (e.g. Netflix, Gym)", enabled, KeyboardOptions(imeAction = ImeAction.Next))
                DialogField(draft.amountDigits, onAmountChange, "Amount (LKR)", enabled, KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next))
                DialogField(draft.dueDayDigits, onDueDayChange, "Due day of the month (1–31)", enabled, KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done))
                ChipGroup(label = "Paid from", options = accountNames, selected = draft.account, enabled = enabled, onSelect = onAccountSelect)
                ChipGroup(label = "Category", options = categoryNames, selected = draft.category, enabled = enabled, onSelect = onCategorySelect)
                draft.errorMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = enabled) {
                Text(if (draft.isNew) "Add" else "Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                if (!draft.isNew) {
                    TextButton(onClick = onDelete, enabled = enabled) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDismiss, enabled = enabled) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun DialogField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    keyboardOptions: KeyboardOptions
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ChipGroup(
    label: String,
    options: List<String>,
    selected: String,
    enabled: Boolean,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    enabled = enabled,
                    shape = RoundedCornerShape(12.dp),
                    label = { Text(option) }
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun BillsListPreview() {
    MoneyTrailTheme {
        BillsList(
            state = RecurringBillsState(
                isLoading = false,
                bills = listOf(
                    RecurringBill("1", "Netflix", 1_990, "Secondary bank", "Subscriptions", 5),
                    RecurringBill("2", "Gym", 6_500, "Main bank", "Health", 1, isActive = false)
                )
            ),
            today = LocalDate.of(2026, 11, 5),
            onEditClick = {},
            onActiveChange = { _, _ -> }
        )
    }
}
