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
import lk.moneytrail.app.presentation.components.formatAmount
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme

@Composable
fun NewGoalDialog(
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
    var name by remember { mutableStateOf(initialGoal?.name ?: "MacBook Pro M4") }
    var targetDigits by remember { mutableStateOf(initialGoal?.targetAmount?.toString() ?: "490000") }
    var savedDigits by remember { mutableStateOf(initialGoal?.savedAmount?.toString() ?: "11200") }
    var monthsDigits by remember { mutableStateOf("12") }
    var transferDayDigits by remember { mutableStateOf(initialGoal?.transferDay?.toString() ?: "25") }
    var allowEmergency by remember { mutableStateOf(initialGoal?.allowEmergencyWithdrawals ?: true) }

    val targetAmount = targetDigits.toLongOrNull() ?: 0L
    val savedAmount = savedDigits.toLongOrNull() ?: 0L
    val months = (monthsDigits.toIntOrNull() ?: 12).coerceAtLeast(1)
    val transferDay = (transferDayDigits.toIntOrNull() ?: 25).coerceIn(1, 28)

    val remainingShortfall = (targetAmount - savedAmount).coerceAtLeast(0L)
    val monthlySavingNeeded = if (remainingShortfall > 0) {
        (remainingShortfall + months - 1) / months
    } else 0L

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
                    onValueChange = { name = it },
                    label = { Text("Goal Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = targetDigits,
                    onValueChange = { if (it.all { char -> char.isDigit() }) targetDigits = it },
                    label = { Text("Target Amount (LKR)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = savedDigits,
                    onValueChange = { if (it.all { char -> char.isDigit() }) savedDigits = it },
                    label = { Text("Current Saved Amount (LKR)") },
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
                            fontSize = 11.sp,
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
                        name.trim().ifEmpty { "Savings Goal" },
                        targetAmount,
                        savedAmount,
                        months,
                        transferDay,
                        allowEmergency
                    )
                },
                enabled = name.isNotBlank() && targetAmount > 0,
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
            onSaveGoal = { _, _, _, _, _, _ -> },
            onDismiss = {}
        )
    }
}
