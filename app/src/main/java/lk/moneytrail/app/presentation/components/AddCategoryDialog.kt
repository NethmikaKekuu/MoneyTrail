package lk.moneytrail.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.SpendingType
import lk.moneytrail.app.domain.usecase.AddCategoryUseCase
import lk.moneytrail.app.presentation.auth.AuthErrorText
import lk.moneytrail.app.presentation.auth.AuthTextField
import lk.moneytrail.app.presentation.theme.Ink

/**
 * Asks for a new category's name and, for an expense, whether spending on it is
 * usually committed or discretionary. With [fixedType] null the user also picks
 * Expense or Income (Settings); otherwise the type is set (new entry, onboarding).
 * What's typed stays in the dialog until Add; the screen's ViewModel checks and saves it.
 */
@Composable
fun AddCategoryDialog(
    fixedType: EntryType?,
    errorMessage: String?,
    isSaving: Boolean,
    onAdd: (name: String, type: EntryType, spendingType: SpendingType?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(fixedType ?: EntryType.EXPENSE) }
    var spendingType by rememberSaveable { mutableStateOf(SpendingType.DISCRETIONARY) }
    val enabled = !isSaving

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (fixedType) {
                    EntryType.EXPENSE -> "New expense category"
                    EntryType.INCOME -> "New income category"
                    EntryType.TRANSFER -> "New transfer category"
                    null -> "New category"
                }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AuthTextField(
                    value = name,
                    onValueChange = { name = it.take(AddCategoryUseCase.MAX_NAME_LENGTH) },
                    label = "Name",
                    placeholder = if (type == EntryType.EXPENSE) "e.g. Pet care" else "e.g. Tuition classes",
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done)
                )
                if (fixedType == null) {
                    ChoiceRow(
                        options = listOf(EntryType.EXPENSE to "Expense", EntryType.INCOME to "Income"),
                        selected = type,
                        onSelect = { type = it }
                    )
                }
                if (type == EntryType.EXPENSE) {
                    Text("Spending on it is usually", style = MaterialTheme.typography.labelMedium)
                    ChoiceRow(
                        options = listOf(SpendingType.COMMITTED to "Committed", SpendingType.DISCRETIONARY to "Discretionary"),
                        selected = spendingType,
                        onSelect = { spendingType = it }
                    )
                }
                if (errorMessage != null) {
                    AuthErrorText(errorMessage)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(name, type, if (type == EntryType.EXPENSE) spendingType else null) },
                enabled = enabled
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** One choice out of a few, as chips that wrap onto a second line when they don't fit. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun <T> ChoiceRow(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    val colors = MaterialTheme.colorScheme
    FlowRow(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            val shape = RoundedCornerShape(20.dp)
            Box(
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .clip(shape)
                    .background(if (isSelected) colors.tertiary else colors.surface)
                    .border(1.5.dp, if (isSelected) colors.tertiary else colors.outlineVariant, shape)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(value) })
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                // The picked chip is always orange, so its text stays dark in both themes.
                Text(label, style = MaterialTheme.typography.labelMedium, color = if (isSelected) Ink else colors.onSurface)
            }
        }
    }
}
