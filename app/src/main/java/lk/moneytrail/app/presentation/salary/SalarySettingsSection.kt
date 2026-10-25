package lk.moneytrail.app.presentation.salary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import lk.moneytrail.app.presentation.auth.AuthErrorText
import lk.moneytrail.app.presentation.auth.AuthPrimaryButton
import lk.moneytrail.app.presentation.auth.AuthTextField
import lk.moneytrail.app.presentation.components.formatAmount
import lk.moneytrail.app.presentation.onboarding.PayDayGrid
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme

/**
 * Change the pay day and the base salary (before bonuses), as onboarding set them. This
 * month's expected salary follows the change.
 */
@Composable
internal fun SalarySettingsSection(
    state: ConfirmSalaryUiState,
    onPayDayClick: (Int) -> Unit,
    onBaseSalaryChange: (String) -> Unit,
    onSaveSalaryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Your pay day and base salary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Pay day", style = MaterialTheme.typography.labelMedium)
        PayDayGrid(selectedDay = state.payDay, onDayClick = onPayDayClick)
        Spacer(Modifier.height(4.dp))
        AuthTextField(
            value = state.baseSalaryDigits,
            onValueChange = onBaseSalaryChange,
            label = "Base salary (LKR)",
            placeholder = "e.g. 120000",
            enabled = !state.isSavingSalary,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
        )
        Text(
            text = state.baseSalaryDigits.toLongOrNull()?.let {
                "LKR ${formatAmount(it)} a month, before bonuses. A month's bonus is whatever arrives above it."
            } ?: "Before bonuses. Enter 0 if you don't get a salary.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.outline
        )
        when {
            state.salarySettingsError != null -> AuthErrorText(state.salarySettingsError)
            state.isSalarySaved -> Text(
                text = "Salary saved.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        AuthPrimaryButton(text = "Save salary", onClick = onSaveSalaryClick, isLoading = state.isSavingSalary)
    }
}

@Preview(showBackground = true)
@Composable
private fun SalarySettingsSectionPreview() {
    MoneyTrailTheme {
        SalarySettingsSection(
            state = ConfirmSalaryUiState(payDay = 25, baseSalaryDigits = "120000", isLoading = false),
            onPayDayClick = {},
            onBaseSalaryChange = {},
            onSaveSalaryClick = {}
        )
    }
}
