package lk.moneytrail.app.presentation.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.SpendingType
import lk.moneytrail.app.presentation.auth.AuthBackButton
import lk.moneytrail.app.presentation.auth.AuthErrorText
import lk.moneytrail.app.presentation.auth.AuthPrimaryButton
import lk.moneytrail.app.presentation.auth.AuthTextField
import lk.moneytrail.app.presentation.calendar.SquareIconButton
import lk.moneytrail.app.presentation.components.ErrorView
import lk.moneytrail.app.presentation.components.LoadingView
import lk.moneytrail.app.presentation.components.MINUS_SIGN
import lk.moneytrail.app.presentation.components.MoneyTrailDatePickerDialog
import lk.moneytrail.app.presentation.components.MoneyTrailIcons
import lk.moneytrail.app.presentation.components.formatAmount
import lk.moneytrail.app.presentation.theme.Ink
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// MoneyTrail-Figma-HTML/24-EditEntry.html
@Composable
fun EditEntryScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditEntryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) onClose()
    }

    EditEntryContent(
        uiState = uiState,
        onBackClick = onClose,
        onRetryClick = viewModel::loadEntry,
        onAmountChange = viewModel::onAmountChange,
        onBonusChange = viewModel::onBonusChange,
        onCategoryChange = viewModel::onCategoryChange,
        onAccountChange = viewModel::onAccountChange,
        onSpendingTypeChange = viewModel::onSpendingTypeChange,
        onDateChange = viewModel::onDateChange,
        onNoteChange = viewModel::onNoteChange,
        onSaveClick = viewModel::save,
        onDeleteClick = viewModel::onDeleteClick,
        onDeleteConfirm = viewModel::onDeleteConfirm,
        onDeleteDismiss = viewModel::onDeleteDismiss,
        modifier = modifier
    )
}

private val ChipDateFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)

@Composable
private fun EditEntryContent(
    uiState: EditEntryState,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onAmountChange: (String) -> Unit,
    onBonusChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onAccountChange: (String) -> Unit,
    onSpendingTypeChange: (SpendingType) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onNoteChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onDeleteDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val original = uiState.original
    val draft = uiState.draft

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
                .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AuthBackButton(onClick = onBackClick, contentDescription = "Back")
            Text(
                text = "Edit entry",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            if (draft != null) {
                SquareIconButton(MoneyTrailIcons.Trash, "Delete entry", onDeleteClick)
            } else {
                Spacer(Modifier.size(44.dp))
            }
        }

        when {
            uiState.isLoading -> LoadingView(message = "Loading the entry…")
            uiState.loadError != null || original == null || draft == null -> ErrorView(
                title = "Can't open this entry",
                message = uiState.loadError ?: "Couldn't load this entry.",
                onRetry = onRetryClick
            )
            else -> {
                EditForm(
                    uiState = uiState,
                    original = original,
                    draft = draft,
                    onAmountChange = onAmountChange,
                    onBonusChange = onBonusChange,
                    onCategoryChange = onCategoryChange,
                    onAccountChange = onAccountChange,
                    onSpendingTypeChange = onSpendingTypeChange,
                    onDateChange = onDateChange,
                    onNoteChange = onNoteChange,
                    modifier = Modifier.weight(1f)
                )
                Column(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (uiState.errorMessage != null) {
                        AuthErrorText(uiState.errorMessage)
                    }
                    AuthPrimaryButton(text = "Save changes", onClick = onSaveClick, isLoading = uiState.isSaving)
                }
            }
        }
    }

    if (uiState.isConfirmingDelete && original != null) {
        AlertDialog(
            onDismissRequest = onDeleteDismiss,
            title = { Text("Delete this entry?") },
            text = {
                Text(
                    "${original.category}, LKR ${formatAmount(original.amount)} on " +
                        "${original.date.format(ChipDateFormatter)} will be removed from your calendar and totals."
                )
            },
            confirmButton = {
                TextButton(onClick = onDeleteConfirm) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDeleteDismiss) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditForm(
    uiState: EditEntryState,
    original: Entry,
    draft: Entry,
    onAmountChange: (String) -> Unit,
    onBonusChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onAccountChange: (String) -> Unit,
    onSpendingTypeChange: (SpendingType) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onNoteChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val enabled = !uiState.isSaving
    val isExpense = draft.type == EntryType.EXPENSE
    var isDatePickerOpen by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "${original.category} · ${if (isExpense) "Expense" else "Income"}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.outline
            )
            Text(
                text = (if (isExpense) MINUS_SIGN else "+") + formatAmount(uiState.amount),
                style = MaterialTheme.typography.displaySmall,
                color = colors.onSurface
            )
            if (uiState.bonus > 0) {
                Text(
                    text = "Base ${formatAmount(uiState.amount - uiState.bonus)} + bonus ${formatAmount(uiState.bonus)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.outline
                )
            }
        }

        AuthTextField(
            value = uiState.amountDigits,
            onValueChange = onAmountChange,
            label = if (uiState.isSalary) "Base salary (LKR)" else "Amount (LKR)",
            placeholder = "0",
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
        )

        // The part of a salary that isn't fixed: added on top of the base pay.
        if (uiState.isSalary) {
            AuthTextField(
                value = uiState.bonusDigits,
                onValueChange = onBonusChange,
                label = "Bonus (LKR, optional)",
                placeholder = "0",
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
            )
        }

        Section(
            label = "Category",
            note = if (draft.category != original.category) "· was ${original.category}" else null
        ) {
            FlowRow(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.categories.forEach { category ->
                    OptionChip(category, selected = category == draft.category, onClick = { onCategoryChange(category) })
                }
            }
        }

        Section(label = "Account") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.accountChoices.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { account ->
                            OptionChip(
                                text = account,
                                selected = account == draft.account,
                                onClick = { onAccountChange(account) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            )
                        }
                        // Keep a short last row's buttons the same width as the others.
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        if (isExpense) {
            Section(label = "Type") {
                SpendingTypeToggle(selected = draft.spendingType, onChange = onSpendingTypeChange)
            }
        }

        Section(label = "Date") {
            // The entry's own day, the day before it, and the one picked if it's neither.
            val dates = (listOf(original.date, original.date.minusDays(1)) + draft.date).distinct().sortedDescending()
            FlowRow(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dates.forEach { date ->
                    OptionChip(date.format(ChipDateFormatter), selected = date == draft.date, onClick = { onDateChange(date) })
                }
                OptionChip("Other date…", selected = false, onClick = { isDatePickerOpen = true })
            }
        }

        AuthTextField(
            value = draft.note,
            onValueChange = onNoteChange,
            label = "Note",
            placeholder = "e.g. Ride home after team lunch",
            enabled = enabled,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done)
        )

        MovesToHint(original = original, draft = draft)
    }

    if (isDatePickerOpen) {
        MoneyTrailDatePickerDialog(
            date = draft.date,
            onDateSelected = onDateChange,
            onDismiss = { isDatePickerOpen = false }
        )
    }
}

/** A bold label, an optional quieter note beside it, and its controls underneath. */
@Composable
private fun Section(label: String, note: String? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = buildAnnotatedString {
                append(label)
                if (note != null) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.outline)) {
                        append(" $note")
                    }
                }
            },
            style = MaterialTheme.typography.labelMedium
        )
        content()
    }
}

@Composable
private fun OptionChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp)
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(shape)
            .background(if (selected) colors.tertiary else colors.surface)
            .border(1.5.dp, if (selected) colors.tertiary else colors.outlineVariant, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            // Selected chips are always orange, so their text stays dark in both themes.
            color = if (selected) Ink else colors.onSurface,
            maxLines = 1
        )
    }
}

@Composable
private fun SpendingTypeToggle(selected: SpendingType?, onChange: (SpendingType) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow, RoundedCornerShape(14.dp))
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(SpendingType.COMMITTED to "Committed", SpendingType.DISCRETIONARY to "Discretionary").forEach { (type, label) ->
            val isSelected = type == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) colors.primary else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onChange(type) }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 14.sp,
                    color = if (isSelected) colors.onPrimary else colors.outline
                )
            }
        }
    }
}

/** "Moves to Expenses › October › Rides" once the folder changes, otherwise where it's filed now. */
@Composable
private fun MovesToHint(original: Entry, draft: Entry) {
    val colors = MaterialTheme.colorScheme
    fun folderOf(entry: Entry): String {
        val folder = if (entry.type == EntryType.EXPENSE) "Expenses" else "Income"
        val month = entry.date.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        return "$folder › $month › ${entry.category}"
    }
    val moves = folderOf(draft) != folderOf(original)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.primaryContainer, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(MoneyTrailIcons.Folder, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
        Text(
            text = buildAnnotatedString {
                append(if (moves) "Moves to " else "Filed in ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(folderOf(draft)) }
                append(if (moves) "; totals and charts update." else ".")
            },
            style = MaterialTheme.typography.bodySmall,
            color = colors.onPrimaryContainer
        )
    }
}

private val PreviewEntry = Entry(
    id = "1",
    type = EntryType.EXPENSE,
    amount = 780,
    category = "Rides",
    account = "Cash wallet",
    date = LocalDate.of(2026, 10, 10),
    note = "Ride home after team lunch",
    spendingType = SpendingType.DISCRETIONARY
)

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun EditEntryScreenPreview() {
    MoneyTrailTheme {
        EditEntryContent(
            uiState = EditEntryState(
                isLoading = false,
                original = PreviewEntry.copy(category = "Other"),
                draft = PreviewEntry,
                amountDigits = "780"
            ),
            onBackClick = {},
            onRetryClick = {},
            onAmountChange = {},
            onBonusChange = {},
            onCategoryChange = {},
            onAccountChange = {},
            onSpendingTypeChange = {},
            onDateChange = {},
            onNoteChange = {},
            onSaveClick = {},
            onDeleteClick = {},
            onDeleteConfirm = {},
            onDeleteDismiss = {}
        )
    }
}
