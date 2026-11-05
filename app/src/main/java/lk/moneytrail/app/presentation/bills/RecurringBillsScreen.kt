package lk.moneytrail.app.presentation.bills

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.moneytrail.app.domain.model.RecurringBill
import lk.moneytrail.app.presentation.auth.AuthErrorText
import lk.moneytrail.app.presentation.auth.AuthTextField
import lk.moneytrail.app.presentation.components.EmptyView
import lk.moneytrail.app.presentation.components.ErrorView
import lk.moneytrail.app.presentation.components.LoadingView
import lk.moneytrail.app.presentation.components.MoneyTrailIcons
import lk.moneytrail.app.presentation.components.formatAmount
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme

/**
 * Recurring bills: the subscriptions and auto-debits that leave an account on the same
 * day every month. Each month an active bill appears on the calendar to be checked with
 * "Did you pay it?".
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
        topBar = { BillsTopBar(onBackClick = onBackClick, onAddClick = viewModel::onAddClick) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                uiState.isLoading -> LoadingView(message = "Loading your bills…")
                uiState.loadError != null -> ErrorView(message = uiState.loadError!!, onRetry = viewModel::onRetryClick)
                uiState.bills.isEmpty() -> EmptyView(
                    title = "No recurring bills yet",
                    message = "Add your subscriptions and auto-debits once. Each month they appear on the calendar, ready to mark as paid.",
                    actionLabel = "Add a bill",
                    onAction = viewModel::onAddClick
                )
                else -> BillsList(
                    state = uiState,
                    onEditClick = viewModel::onEditClick,
                    onActiveChange = viewModel::onActiveChange,
                    onDeleteClick = viewModel::onDeleteClick
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
            onDismiss = viewModel::onDraftDismiss
        )
    }

    uiState.deleting?.let { bill ->
        AlertDialog(
            onDismissRequest = viewModel::onDeleteDismiss,
            title = { Text("Delete this bill?") },
            text = { Text("${bill.name} will stop appearing on the calendar. Entries it already made stay.") },
            confirmButton = {
                TextButton(onClick = viewModel::onDeleteConfirm) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = viewModel::onDeleteDismiss) { Text("Cancel") } }
        )
    }
}

@Composable
private fun BillsTopBar(onBackClick: () -> Unit, onAddClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBox(icon = MoneyTrailIcons.ChevronLeft, description = "Back", onClick = onBackClick)
        Text(
            text = "Recurring bills",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        IconBox(icon = MoneyTrailIcons.Plus, description = "Add a bill", onClick = onAddClick)
    }
}

@Composable
private fun IconBox(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = description, tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun BillsList(
    state: RecurringBillsState,
    onEditClick: (RecurringBill) -> Unit,
    onActiveChange: (RecurringBill, Boolean) -> Unit,
    onDeleteClick: (RecurringBill) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Every month",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "LKR ${formatAmount(state.monthlyTotal)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
        items(state.bills, key = { it.id }) { bill ->
            BillCard(
                bill = bill,
                onEditClick = { onEditClick(bill) },
                onActiveChange = { onActiveChange(bill, it) },
                onDeleteClick = { onDeleteClick(bill) }
            )
        }
    }
}

@Composable
private fun BillCard(
    bill: RecurringBill,
    onEditClick: () -> Unit,
    onActiveChange: (Boolean) -> Unit,
    onDeleteClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(18.dp))
            .clickable(onClick = onEditClick)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = bill.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (bill.isActive) colors.onSurface else colors.outline
                )
                Text(
                    text = "LKR ${formatAmount(bill.amount)} · day ${bill.dueDay}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
                Text(
                    text = "${bill.category} · ${bill.account}" + if (bill.isActive) "" else " · Stopped",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.outline
                )
            }
            Switch(checked = bill.isActive, onCheckedChange = onActiveChange)
            IconButton(onClick = onDeleteClick) {
                Icon(MoneyTrailIcons.Trash, contentDescription = "Delete ${bill.name}", tint = colors.outline)
            }
        }
    }
}

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
    onDismiss: () -> Unit
) {
    val enabled = !isSaving
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (draft.isNew) "New recurring bill" else "Edit bill") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AuthTextField(
                    value = draft.name,
                    onValueChange = onNameChange,
                    label = "Name",
                    placeholder = "e.g. Netflix, Gym",
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                AuthTextField(
                    value = draft.amountDigits,
                    onValueChange = onAmountChange,
                    label = "Amount (LKR)",
                    placeholder = "0",
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                )
                AuthTextField(
                    value = draft.dueDayDigits,
                    onValueChange = onDueDayChange,
                    label = "Due day of the month (1-31)",
                    placeholder = "e.g. 5",
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                )
                ChipGroup(label = "Paid from", options = accountNames, selected = draft.account, enabled = enabled, onSelect = onAccountSelect)
                ChipGroup(label = "Category", options = categoryNames, selected = draft.category, enabled = enabled, onSelect = onCategorySelect)
                if (draft.errorMessage != null) {
                    AuthErrorText(draft.errorMessage)
                }
            }
        },
        confirmButton = { TextButton(onClick = onSave, enabled = enabled) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = enabled) { Text("Cancel") } }
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
        Text(label, style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    enabled = enabled,
                    label = { Text(option) }
                )
            }
            Spacer(modifier = Modifier.size(1.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BillCardPreview() {
    MoneyTrailTheme {
        BillCard(
            bill = RecurringBill("1", "Netflix", 1_990, "Secondary bank", "Other", 5),
            onEditClick = {},
            onActiveChange = {},
            onDeleteClick = {}
        )
    }
}
