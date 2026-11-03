package lk.moneytrail.app.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.presentation.auth.AuthErrorText
import lk.moneytrail.app.presentation.auth.AuthTextField
import lk.moneytrail.app.presentation.components.MoneyTrailDatePickerDialog
import lk.moneytrail.app.presentation.components.MoneyTrailIcons
import lk.moneytrail.app.presentation.components.formatAmount
import lk.moneytrail.app.presentation.theme.Ink
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Checking expected entries on their day: the card at the top of the calendar and
// the "Did it come in?" dialog it opens.

private val CheckDateFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)

private fun whenExpected(date: LocalDate, today: LocalDate): String =
    if (date == today) "today" else "on ${date.format(CheckDateFormatter)}"

private fun question(entry: Entry): String =
    if (entry.type == EntryType.INCOME) "Did it come in?" else "Did you pay it?"

/** What the check dialog can do, gathered so the calendar screen passes one value. */
internal class ExpectedCheckActions(
    val onAmountChange: (String) -> Unit,
    val onBonusChange: (String) -> Unit,
    val onDateChange: (LocalDate) -> Unit,
    val onConfirm: () -> Unit,
    val onNotYet: () -> Unit,
    val onReschedule: (LocalDate) -> Unit,
    val onNewDateDismiss: () -> Unit,
    val onRemove: () -> Unit,
    val onDismiss: () -> Unit
)

/**
 * Confirm with what really arrived or was paid (it often differs: AdSense, bonuses),
 * move it to a later date, or remove it if it didn't happen.
 */
@Composable
internal fun ExpectedCheckDialog(
    check: ExpectedCheck,
    today: LocalDate,
    actions: ExpectedCheckActions
) {
    val colors = MaterialTheme.colorScheme
    val entry = check.entry
    val isIncome = entry.type == EntryType.INCOME
    val enabled = !check.isSaving

    AlertDialog(
        onDismissRequest = actions.onDismiss,
        title = { Text(question(entry)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "${entry.category} · LKR ${formatAmount(entry.amount)} expected ${whenExpected(entry.date, today)}" +
                        if (entry.note.isNotBlank()) "\n“${entry.note}”" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
                AuthTextField(
                    value = check.amountDigits,
                    onValueChange = actions.onAmountChange,
                    label = when {
                        check.isSalary -> "Base salary received (LKR)"
                        isIncome -> "Amount received (LKR)"
                        else -> "Amount paid (LKR)"
                    },
                    placeholder = "0",
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                )
                if (check.isSalary) {
                    AuthTextField(
                        value = check.bonusDigits,
                        onValueChange = actions.onBonusChange,
                        label = "Bonus this month (LKR, optional)",
                        placeholder = "0",
                        enabled = enabled,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                    )
                    Text(
                        text = if (check.bonus > 0) {
                            "Added to this salary: LKR ${formatAmount(check.total)} in all."
                        } else {
                            "Added to this salary. You can also add it later by opening the salary."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Normal,
                        color = colors.outline
                    )
                }
                Text(if (isIncome) "Received on" else "Paid on", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(entry.date, today).distinct().forEach { date ->
                        DateChip(
                            text = if (date == today) "Today" else date.format(CheckDateFormatter),
                            selected = date == check.date,
                            onClick = { actions.onDateChange(date) }
                        )
                    }
                }
                if (check.errorMessage != null) {
                    AuthErrorText(check.errorMessage)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = actions.onConfirm, enabled = enabled) { Text("Confirm") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = actions.onNotYet, enabled = enabled) { Text("Not yet") }
                TextButton(onClick = actions.onRemove, enabled = enabled) {
                    Text("Didn't happen", color = colors.error)
                }
            }
        }
    )

    if (check.isPickingNewDate) {
        MoneyTrailDatePickerDialog(
            date = today.plusDays(1),
            onDateSelected = actions.onReschedule,
            onDismiss = actions.onNewDateDismiss
        )
    }
}

@Composable
private fun DateChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(shape)
            .background(if (selected) colors.tertiary else colors.surface)
            .border(1.5.dp, if (selected) colors.tertiary else colors.outlineVariant, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        // The picked chip is always orange, so its text stays dark in both themes.
        Text(text, style = MaterialTheme.typography.labelMedium, color = if (selected) Ink else colors.onSurface)
    }
}
