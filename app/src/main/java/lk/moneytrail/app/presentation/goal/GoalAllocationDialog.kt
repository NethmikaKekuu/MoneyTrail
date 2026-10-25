package lk.moneytrail.app.presentation.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import lk.moneytrail.app.domain.model.GoalModel
import lk.moneytrail.app.presentation.components.formatAmount
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme
import java.time.LocalDate

@Composable
fun GoalAllocationDialog(
    totalPoolAmount: Long,
    goals: List<GoalModel>,
    isSubmitting: Boolean,
    onConfirmAllocation: (allocations: Map<String, Long>) -> Unit,
    onDismiss: () -> Unit
) {
    // Distribute suggested default equally or by recommended monthly saving
    val allocations = remember(goals, totalPoolAmount) {
        mutableStateMapOf<String, String>().apply {
            goals.forEach { goal ->
                this[goal.name] = goal.monthlySavingNeeded.toString()
            }
        }
    }

    val currentAllocatedSum = allocations.values.sumOf { it.toLongOrNull() ?: 0L }
    val unallocatedRemaining = totalPoolAmount - currentAllocatedSum

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Distribute Salary to Goals",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Allocate your confirmed salary savings across active goals.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Summary Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Allocation Pool",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "LKR ${formatAmount(totalPoolAmount)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Unallocated",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "LKR ${formatAmount(unallocatedRemaining)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (unallocatedRemaining >= 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // List of Goal Allocation Inputs
                goals.forEach { goal ->
                    val currentInput = allocations[goal.name].orEmpty()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = goal.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Needed: LKR ${formatAmount(goal.monthlySavingNeeded)}/mo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        OutlinedTextField(
                            value = currentInput,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() }) {
                                    allocations[goal.name] = input
                                }
                            },
                            label = { Text("Amount for ${goal.name} (LKR)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedAllocations = allocations.mapValues { it.value.toLongOrNull() ?: 0L }
                        .filterValues { it > 0L }
                    onConfirmAllocation(parsedAllocations)
                },
                enabled = currentAllocatedSum > 0 && !isSubmitting,
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Allocate & Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun GoalAllocationDialogPreview() {
    MoneyTrailTheme {
        GoalAllocationDialog(
            totalPoolAmount = 39900L,
            goals = listOf(
                GoalModel("MacBook Pro M4", 490000L, 11200L, LocalDate.of(2027, 10, 19)),
                GoalModel("Emergency Fund", 200000L, 50000L, LocalDate.of(2027, 6, 1))
            ),
            isSubmitting = false,
            onConfirmAllocation = {},
            onDismiss = {}
        )
    }
}
