package lk.moneytrail.app.presentation.salary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.moneytrail.app.presentation.components.EmptyView
import lk.moneytrail.app.presentation.components.ErrorView
import lk.moneytrail.app.presentation.components.LoadingView
import lk.moneytrail.app.presentation.components.MoneyTrailIcons
import lk.moneytrail.app.presentation.onboarding.PayDayGrid
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme

// The pay day and base salary, kept minimal. The salary itself is confirmed on the
// calendar, and bonuses belong in the income insights.
@Composable
fun SalaryScreen(
    onBackClick: () -> Unit,
    onSetUpClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SalaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SalaryContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onRetryClick = viewModel::loadPlan,
        onSetUpClick = onSetUpClick,
        onPayDayClick = viewModel::onPayDayClick,
        onBaseSalaryChange = viewModel::onBaseSalaryChange,
        onSaveClick = viewModel::onSaveClick,
        modifier = modifier
    )
}

@Composable
private fun SalaryContent(
    uiState: SalaryState,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onSetUpClick: () -> Unit,
    onPayDayClick: (Int) -> Unit,
    onBaseSalaryChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding()
    ) {
        SalaryTopBar(onBackClick = onBackClick)

        when {
            uiState.isLoading -> LoadingView(message = "Loading your salary…")
            uiState.loadError != null -> ErrorView(
                title = "Couldn't load your salary",
                message = uiState.loadError,
                onRetry = onRetryClick
            )
            !uiState.hasPlan -> EmptyView(
                title = "No salary set up",
                message = "You skipped setup, so there's no salary or savings goal yet.",
                actionLabel = "Set up salary and goal",
                onAction = onSetUpClick
            )
            else -> Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Your salary goes on the calendar on pay day. Confirm it there when it arrives.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text("Pay day", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                PayDayGrid(selectedDay = uiState.selectedPayDay, onDayClick = onPayDayClick)

                OutlinedTextField(
                    value = uiState.baseSalaryDigits,
                    onValueChange = onBaseSalaryChange,
                    label = { Text("Base salary (LKR)") },
                    supportingText = { Text("Before bonuses. Enter 0 if you don't get a salary.") },
                    singleLine = true,
                    enabled = !uiState.isSaving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                when {
                    uiState.errorMessage != null -> Text(
                        text = uiState.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    uiState.isSaved -> Text(
                        text = "Saved. This month's expected salary follows the change.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (uiState.isChanged) {
                    Button(
                        onClick = onSaveClick,
                        enabled = !uiState.isSaving,
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SalaryTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                .clickable(onClick = onBackClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = MoneyTrailIcons.ChevronLeft,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = "Salary",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .padding(end = 44.dp)
        )
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun SalaryScreenPreview() {
    MoneyTrailTheme {
        SalaryContent(
            // Sample figures for the preview only, from the assignment scenario.
            uiState = SalaryState(
                isLoading = false,
                payDay = 25,
                baseSalary = 120_000L,
                selectedPayDay = 28,
                baseSalaryDigits = "120000"
            ),
            onBackClick = {},
            onRetryClick = {},
            onSetUpClick = {},
            onPayDayClick = {},
            onBaseSalaryChange = {},
            onSaveClick = {}
        )
    }
}
