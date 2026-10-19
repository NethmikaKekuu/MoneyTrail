package lk.moneytrail.app.presentation.entry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.presentation.components.MINUS_SIGN
import lk.moneytrail.app.presentation.components.MoneyTrailIcons
import lk.moneytrail.app.presentation.components.formatAmount
import lk.moneytrail.app.presentation.theme.Ink
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// MoneyTrail-Figma-HTML/08-AddEntry.html
@Composable
fun AddEntryScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddEntryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onClose()
    }

    AddEntryContent(
        uiState = uiState,
        onTypeChange = viewModel::onTypeChange,
        onKeyPress = viewModel::onKeyPress,
        onDeletePress = viewModel::onDeletePress,
        onCategoryChange = viewModel::onCategoryChange,
        onAccountChange = viewModel::onAccountChange,
        onDateChange = viewModel::onDateChange,
        onNoteChange = viewModel::onNoteChange,
        onSaveClick = viewModel::save,
        onCloseClick = onClose,
        modifier = modifier
    )
}

private val DateButtonFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)

@Composable
private fun AddEntryContent(
    uiState: AddEntryState,
    onTypeChange: (EntryType) -> Unit,
    onKeyPress: (String) -> Unit,
    onDeletePress: () -> Unit,
    onCategoryChange: (String) -> Unit,
    onAccountChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onNoteChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    var isDatePickerOpen by rememberSaveable { mutableStateOf(false) }

    // Scrolls on short screens; on taller ones the keypad sits at the bottom.
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceContainerLow)
            .systemBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedIconButton(
                    onClick = onCloseClick,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, colors.outlineVariant),
                    colors = IconButtonDefaults.outlinedIconButtonColors(containerColor = colors.surface),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(MoneyTrailIcons.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                }
                Text(
                    text = "New entry",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(44.dp))
            }

            TypeToggle(
                selected = uiState.type,
                onTypeChange = onTypeChange,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp)
            )

            AmountDisplay(
                type = uiState.type,
                amount = uiState.amount,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp)
            )

            CategoryChips(
                categories = uiState.categories,
                selected = uiState.category,
                onCategoryChange = onCategoryChange,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp)
            )

            Row(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AccountPicker(
                    account = uiState.account,
                    accounts = uiState.accounts,
                    onAccountChange = onAccountChange,
                    modifier = Modifier.weight(1f)
                )
                PickerButton(
                    icon = MoneyTrailIcons.Calendar,
                    text = uiState.date.format(DateButtonFormatter),
                    contentDescription = "Date, ${uiState.date.format(DateButtonFormatter)}",
                    onClick = { isDatePickerOpen = true },
                    modifier = Modifier.weight(1f)
                )
            }

            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Note (optional)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.outline
                )
                OutlinedTextField(
                    value = uiState.note,
                    onValueChange = onNoteChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    placeholder = {
                        Text("e.g. Farewell lunch for a teammate", style = MaterialTheme.typography.bodyMedium, fontSize = 14.sp)
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.surface,
                        unfocusedContainerColor = colors.surface,
                        focusedBorderColor = colors.tertiary,
                        unfocusedBorderColor = colors.outlineVariant,
                        focusedPlaceholderColor = colors.outline,
                        unfocusedPlaceholderColor = colors.outline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            FolderHint(
                type = uiState.type,
                category = uiState.category,
                date = uiState.date,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp)
            )

            Spacer(Modifier.weight(1f))

            Keypad(
                onKeyPress = onKeyPress,
                onDeletePress = onDeletePress,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp)
            )

            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 10.dp)
                )
            }

            Button(
                onClick = {
                    focusManager.clearFocus()
                    onSaveClick()
                },
                enabled = !uiState.isSaving,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = colors.primary.copy(alpha = 0.6f),
                    disabledContentColor = colors.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 20.dp)
                    .height(54.dp)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = LocalContentColor.current,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(MoneyTrailIcons.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Save to calendar",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }

    if (isDatePickerOpen) {
        EntryDatePickerDialog(
            date = uiState.date,
            onDateSelected = onDateChange,
            onDismiss = { isDatePickerOpen = false }
        )
    }
}

/** Expense / Income switch at the top. */
@Composable
private fun TypeToggle(
    selected: EntryType,
    onTypeChange: (EntryType) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, shape)
            .border(1.dp, colors.outlineVariant, shape)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(EntryType.EXPENSE to "Expense", EntryType.INCOME to "Income").forEach { (type, label) ->
            val isSelected = type == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) colors.primary else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onTypeChange(type) }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 15.sp,
                    color = if (isSelected) colors.onPrimary else colors.outline
                )
            }
        }
    }
}

@Composable
private fun AmountDisplay(type: EntryType, amount: Long, modifier: Modifier = Modifier) {
    val sign = when {
        amount == 0L -> ""
        type == EntryType.EXPENSE -> MINUS_SIGN
        else -> "+"
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "Amount (LKR)",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = sign + formatAmount(amount),
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp, letterSpacing = (-0.02).em),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChips(
    categories: List<String>,
    selected: String,
    onCategoryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            val isSelected = category == selected
            val shape = RoundedCornerShape(19.dp)
            Box(
                modifier = Modifier
                    .height(38.dp)
                    .clip(shape)
                    .background(if (isSelected) colors.tertiary else colors.surface)
                    .border(1.5.dp, if (isSelected) colors.tertiary else colors.outlineVariant, shape)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onCategoryChange(category) })
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    // The selected chip is always orange, so its text stays dark in both themes.
                    color = if (isSelected) Ink else colors.onSurface
                )
            }
        }
    }
}

@Composable
private fun AccountPicker(
    account: String,
    accounts: List<String>,
    onAccountChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        PickerButton(
            icon = MoneyTrailIcons.Card,
            text = account,
            contentDescription = "Account, $account",
            onClick = { isMenuOpen = true },
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }) {
            accounts.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onAccountChange(option)
                        isMenuOpen = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PickerButton(
    icon: ImageVector,
    text: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, colors.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = colors.surface, contentColor = colors.onSurface),
        contentPadding = PaddingValues(horizontal = 12.dp),
        modifier = modifier
            .height(48.dp)
            .semantics { this.contentDescription = contentDescription }
    ) {
        Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

/** "Files automatically to Expenses › October › Food out", so the user knows where it goes. */
@Composable
private fun FolderHint(type: EntryType, category: String, date: LocalDate, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val folder = if (type == EntryType.EXPENSE) "Expenses" else "Income"
    val month = date.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.primaryContainer, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(MoneyTrailIcons.Folder, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
        Text(
            text = buildAnnotatedString {
                append("Files automatically to ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("$folder › $month › $category") }
            },
            style = MaterialTheme.typography.bodySmall,
            color = colors.onPrimaryContainer
        )
    }
}

private val KeypadRows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))

@Composable
private fun Keypad(onKeyPress: (String) -> Unit, onDeletePress: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        KeypadRows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { key -> KeypadKey(onClick = { onKeyPress(key) }, Modifier.weight(1f)) { KeyLabel(key) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            KeypadKey(onClick = { onKeyPress("00") }, Modifier.weight(1f)) { KeyLabel("00") }
            KeypadKey(onClick = { onKeyPress("0") }, Modifier.weight(1f)) { KeyLabel("0") }
            KeypadKey(onClick = onDeletePress, Modifier.weight(1f)) {
                Icon(MoneyTrailIcons.Backspace, contentDescription = "Delete digit", modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun KeypadKey(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.height(46.dp)
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun KeyLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryDatePickerDialog(
    date: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    // The date picker works in UTC milliseconds at midnight.
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let { millis ->
                    onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        DatePicker(state = pickerState)
    }
}

@Preview(showBackground = true, heightDp = 860)
@Composable
private fun AddEntryScreenPreview() {
    MoneyTrailTheme {
        AddEntryContent(
            uiState = AddEntryState(
                amountDigits = "2400",
                category = "Food out",
                date = LocalDate.of(2026, 10, 10),
                note = "Farewell lunch for a teammate"
            ),
            onTypeChange = {},
            onKeyPress = {},
            onDeletePress = {},
            onCategoryChange = {},
            onAccountChange = {},
            onDateChange = {},
            onNoteChange = {},
            onSaveClick = {},
            onCloseClick = {}
        )
    }
}
