package lk.moneytrail.app.presentation.salary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme

/**
 * Change the pay day and the base salary (before bonuses), as onboarding set them. This
 * month's expected salary follows the change.
 */
@Composable
internal fun EditSalaryDialog(
    state: ConfirmSalaryUiState,
    onPayDayChange: (String) -> Unit,
    onBaseSalaryChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!state.isSavingSalary) onDismiss() },
        title = { Text("Pay day and base salary") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.payDayDigits,
                    onValueChange = onPayDayChange,
                    label = { Text("Pay day (1–31)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.baseSalaryDigits,
                    onValueChange = onBaseSalaryChange,
                    label = { Text("Base salary (LKR)") },
                    supportingText = { Text("Before bonuses. Enter 0 if you don't get a salary.") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.salarySettingsError != null) {
                    Text(
                        text = state.salarySettingsError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = !state.isSavingSalary) {
                if (state.isSavingSalary) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isSavingSalary) { Text("Cancel") }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun EditSalaryDialogPreview() {
    MoneyTrailTheme {
        EditSalaryDialog(
            state = ConfirmSalaryUiState(payDayDigits = "25", baseSalaryDigits = "120000", isLoading = false),
            onPayDayChange = {},
            onBaseSalaryChange = {},
            onSave = {},
            onDismiss = {}
        )
    }
}
