package lk.moneytrail.app.presentation.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lk.moneytrail.app.domain.model.SavingsGoal
import lk.moneytrail.app.domain.model.monthlySavingNeeded
import lk.moneytrail.app.presentation.components.formatAmount
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

// The limits the Firestore rules check on a goal.
private const val MAX_NAME_LENGTH = 60
private const val MAX_DIGITS = 9

@Composable
fun NewGoalDialog(
    /** The plan's pay day, offered as the transfer day; null for someone with no salary set up. */
    defaultTransferDay: Int?,
    initialGoal: SavingsGoal? = null,
    onSaveGoal: (
        name: String,
        targetAmount: Long,
        savedAmount: Long,
        monthsToDeadline: Int,
        transferDay: Int,
        allowEmergency: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    // A new goal starts empty: the user types their own goal, not the scenario's.
    var name by remember { mutableStateOf(initialGoal?.name.orEmpty()) }
    var targetDigits by remember { mutableStateOf(initialGoal?.targetAmount?.toString().orEmpty()) }
    var savedDigits by remember { mutableStateOf(initialGoal?.savedAmount?.toString().orEmpty()) }
    // Editing starts from the goal's own deadline; a new goal from a year away.
    var monthsDigits by remember {
        mutableStateOf(
            initialGoal?.let { ChronoUnit.MONTHS.between(YearMonth.now(), YearMonth.from(it.targetDate)).coerceAtLeast(1) }
                ?.toString() ?: "12"
        )
    }
    var transferDayDigits by remember {
        mutableStateOf((initialGoal?.transferDay ?: defaultTransferDay)?.toString().orEmpty())
    }
    var allowEmergency by remember { mutableStateOf(initialGoal?.allowEmergencyWithdrawals ?: true) }

    val targetAmount = targetDigits.toLongOrNull() ?: 0L
    val savedAmount = savedDigits.toLongOrNull() ?: 0L
    val months = (monthsDigits.toIntOrNull() ?: 12).coerceAtLeast(1)
    val transferDay = (transferDayDigits.toIntOrNull() ?: defaultTransferDay ?: 1).coerceIn(1, 28)

    // The same rule the Firestore rules check: what's saved can't be more than the goal.
    val isSavedTooMuch = savedAmount > targetAmount
    val canSave = name.isNotBlank() && targetAmount > 0 && !isSavedTooMuch
    val today = LocalDate.now()
    val monthlySavingNeeded = SavingsGoal(name, targetAmount, savedAmount, today.plusMonths(months.toLong()))
        .monthlySavingNeeded(today)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialGoal == null) "New Savings Goal" else "Edit Savings Goal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(MAX_NAME_LENGTH) },
                    label = { Text("Goal Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = targetDigits,
                    onValueChange = { if (it.all { char -> char.isDigit() }) targetDigits = it.take(MAX_DIGITS) },
                    label = { Text("Target Amount (LKR)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = savedDigits,
                    onValueChange = { if (it.all { char -> char.isDigit() }) savedDigits = it.take(MAX_DIGITS) },
                    label = { Text("Current Saved Amount (LKR)") },
                    isError = isSavedTooMuch,
                    supportingText = if (isSavedTooMuch) {
                        { Text("Can't be more than the target.") }
                    } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = monthsDigits,
                    onValueChange = { if (it.all { char -> char.isDigit() }) monthsDigits = it },
                    label = { Text("Months to Deadline") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Dynamic Monthly Calculation Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Monthly Target from Salary",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "LKR ${formatAmount(monthlySavingNeeded)} / month",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Formula: (${formatAmount(targetAmount)} - ${formatAmount(savedAmount)}) ÷ $months months = LKR ${formatAmount(monthlySavingNeeded)}/mo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                OutlinedTextField(
                    value = transferDayDigits,
                    onValueChange = { if (it.all { char -> char.isDigit() }) transferDayDigits = it },
                    label = { Text("Transfer Day of Month (1-28)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Checkbox(
                        checked = allowEmergency,
                        onCheckedChange = { allowEmergency = it }
                    )
                    Text(
                        text = "Allow emergency withdrawals from this goal",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveGoal(
                        name.trim(),
                        targetAmount,
                        savedAmount,
                        months,
                        transferDay,
                        allowEmergency
                    )
                },
                enabled = canSave,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Goal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun NewGoalDialogPreview() {
    MoneyTrailTheme {
        NewGoalDialog(
            defaultTransferDay = 25,
            onSaveGoal = { _, _, _, _, _, _ -> },
            onDismiss = {}
        )
    }
}
