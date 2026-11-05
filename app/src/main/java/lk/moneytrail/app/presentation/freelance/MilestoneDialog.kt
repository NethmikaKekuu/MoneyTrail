package lk.moneytrail.app.presentation.freelance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import lk.moneytrail.app.presentation.components.MoneyTrailDatePickerDialog
import lk.moneytrail.app.presentation.components.formatAmount
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** What the milestone dialog hands back when the user saves. */
data class MilestoneInput(val title: String, val amount: Long, val dueDate: LocalDate)

/**
 * Why a milestone can't be saved, or null if it can. [maxAmount] is how much of the
 * project price is still free for this milestone (null when there is no limit).
 */
fun milestoneError(title: String, amount: Long, maxAmount: Long?): String? = when {
    title.isBlank() -> "Enter a milestone title."
    amount <= 0 -> "Enter an amount greater than 0."
    maxAmount != null && amount > maxAmount ->
        if (maxAmount <= 0) "The whole price is already split into milestones."
        else "Only ${formatAmount(maxAmount)} of the price is left to split."
    else -> null
}

/**
 * Add or edit one milestone: title, amount and due date.
 * [initialAmount] starts the amount (e.g. what's left to split); [maxAmount] caps it.
 */
@Composable
fun MilestoneDialog(
    heading: String,
    confirmLabel: String,
    initialTitle: String,
    initialAmount: Long,
    initialDueDate: LocalDate,
    maxAmount: Long?,
    onSave: (MilestoneInput) -> Unit,
    onDismiss: () -> Unit,
    onRemove: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(initialTitle) }
    var amountDigits by remember { mutableStateOf(if (initialAmount > 0) initialAmount.toString() else "") }
    var dueDate by remember { mutableStateOf(initialDueDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(heading, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; errorText = null },
                    label = { Text("Title (e.g. Design, Build)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountDigits,
                    onValueChange = { amountDigits = it.filter(Char::isDigit); errorText = null },
                    label = { Text("Amount (LKR)") },
                    supportingText = maxAmount?.takeIf { it > 0 }?.let { { Text("${formatAmount(it)} left to split") } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Due ${dueDate.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH))}")
                }
                errorText?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = amountDigits.toLongOrNull() ?: 0L
                val error = milestoneError(title, amount, maxAmount)
                if (error != null) errorText = error
                else onSave(MilestoneInput(title.trim(), amount, dueDate))
            }) { Text(confirmLabel, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            Row {
                if (onRemove != null) {
                    TextButton(onClick = onRemove) { Text("Remove", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )

    if (showDatePicker) {
        MoneyTrailDatePickerDialog(
            date = dueDate,
            onDateSelected = { dueDate = it },
            onDismiss = { showDatePicker = false }
        )
    }
}
