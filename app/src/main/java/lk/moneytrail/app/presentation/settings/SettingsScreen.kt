package lk.moneytrail.app.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.moneytrail.app.domain.model.CustomCategory
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.SpendingType
import lk.moneytrail.app.presentation.auth.AuthBackButton
import lk.moneytrail.app.presentation.auth.AuthErrorText
import lk.moneytrail.app.presentation.auth.AuthPrimaryButton
import lk.moneytrail.app.presentation.auth.AuthSecondaryButton
import lk.moneytrail.app.presentation.auth.AuthTextField
import lk.moneytrail.app.presentation.components.AddCategoryDialog
import lk.moneytrail.app.presentation.components.ErrorView
import lk.moneytrail.app.presentation.components.LoadingView
import lk.moneytrail.app.presentation.components.MoneyTrailIcons
import lk.moneytrail.app.presentation.components.formatAmount
import lk.moneytrail.app.presentation.onboarding.PayDayGrid
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme

// Not in the Figma file; follows the style of the onboarding and edit entry screens.
// Changes what onboarding set up: the salary and pay day, and the user's own categories.
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onSetUpClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onRetryClick = viewModel::loadPlan,
        onSetUpClick = onSetUpClick,
        onPayDayClick = viewModel::onPayDayClick,
        onBaseSalaryChange = viewModel::onBaseSalaryChange,
        onSaveSalaryClick = viewModel::onSaveSalaryClick,
        onAddCategoryClick = viewModel::onAddCategoryClick,
        onAddCategory = viewModel::onAddCategory,
        onAddCategoryDismiss = viewModel::onAddCategoryDismiss,
        onDeleteCategoryClick = viewModel::onDeleteCategoryClick,
        onDeleteCategoryConfirm = viewModel::onDeleteCategoryConfirm,
        onDeleteCategoryDismiss = viewModel::onDeleteCategoryDismiss,
        modifier = modifier
    )
}

@Composable
private fun SettingsContent(
    uiState: SettingsState,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onSetUpClick: () -> Unit,
    onPayDayClick: (Int) -> Unit,
    onBaseSalaryChange: (String) -> Unit,
    onSaveSalaryClick: () -> Unit,
    onAddCategoryClick: () -> Unit,
    onAddCategory: (String, EntryType, SpendingType?) -> Unit,
    onAddCategoryDismiss: () -> Unit,
    onDeleteCategoryClick: (String) -> Unit,
    onDeleteCategoryConfirm: () -> Unit,
    onDeleteCategoryDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AuthBackButton(onClick = onBackClick, contentDescription = "Back")
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.size(44.dp))
        }

        when {
            uiState.isLoading -> LoadingView(message = "Loading your settings…")
            uiState.loadError != null -> ErrorView(
                title = "Can't open settings",
                message = uiState.loadError,
                onRetry = onRetryClick
            )
            else -> Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionTitle("Salary")
                if (uiState.hasPlan) {
                    SalarySection(uiState, onPayDayClick, onBaseSalaryChange, onSaveSalaryClick)
                } else {
                    Text(
                        text = "You skipped setup, so there's no salary or savings goal yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AuthSecondaryButton(text = "Set up salary and goal", onClick = onSetUpClick)
                }

                Spacer(Modifier.height(14.dp))
                SectionTitle("Your categories")
                CategoriesSection(uiState, onAddCategoryClick, onDeleteCategoryClick)
            }
        }
    }

    if (uiState.isAddingCategory) {
        AddCategoryDialog(
            fixedType = null,
            errorMessage = uiState.categoryError,
            isSaving = uiState.isSavingCategory,
            onAdd = onAddCategory,
            onDismiss = onAddCategoryDismiss
        )
    }

    uiState.categoryToDelete?.let { category ->
        AlertDialog(
            onDismissRequest = onDeleteCategoryDismiss,
            title = { Text("Delete ${category.name}?") },
            text = {
                Text("It won't be offered for new entries. Entries already saved keep it.")
            },
            confirmButton = {
                TextButton(onClick = onDeleteCategoryConfirm) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDeleteCategoryDismiss) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun ColumnScope.SalarySection(
    uiState: SettingsState,
    onPayDayClick: (Int) -> Unit,
    onBaseSalaryChange: (String) -> Unit,
    onSaveSalaryClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Text("Pay day", style = MaterialTheme.typography.labelMedium)
    PayDayGrid(selectedDay = uiState.payDay, onDayClick = onPayDayClick)
    Spacer(Modifier.height(4.dp))
    AuthTextField(
        value = uiState.baseSalaryDigits,
        onValueChange = onBaseSalaryChange,
        label = "Base salary (LKR)",
        placeholder = "e.g. 120000",
        enabled = !uiState.isSavingSalary,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
    )
    Text(
        text = uiState.baseSalary?.let {
            "LKR ${formatAmount(it)} a month, before bonuses. A month's bonus goes on that salary: " +
                "add it when you confirm the salary, or open the salary on the calendar."
        }
            ?: "Before bonuses. Enter 0 if you don't get a salary.",
        style = MaterialTheme.typography.bodySmall,
        color = colors.outline
    )
    when {
        uiState.salaryError != null -> AuthErrorText(uiState.salaryError)
        uiState.isSalarySaved -> Text(
            text = "Salary saved.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
    AuthPrimaryButton(text = "Save salary", onClick = onSaveSalaryClick, isLoading = uiState.isSavingSalary)
}

@Composable
private fun ColumnScope.CategoriesSection(
    uiState: SettingsState,
    onAddCategoryClick: () -> Unit,
    onDeleteCategoryClick: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = "Added on top of the built-in ones like Food out and Salary, which stay.",
        style = MaterialTheme.typography.bodySmall,
        color = colors.outline
    )
    if (uiState.categories.isEmpty()) {
        Text(
            text = "None yet. Add one here, or with + New when you add an entry.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 6.dp)
        )
    }
    uiState.categories.forEach { category ->
        CategoryRow(category = category, onDeleteClick = { onDeleteCategoryClick(category.id) })
    }
    if (uiState.categoriesError != null) {
        AuthErrorText(uiState.categoriesError)
    }
    OutlinedButton(
        onClick = onAddCategoryClick,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, colors.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Icon(MoneyTrailIcons.Plus, contentDescription = null, modifier = Modifier.size(18.dp))
        Text("Add category", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun CategoryRow(category: CustomCategory, onDeleteClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(colors.surfaceContainerLow, RoundedCornerShape(16.dp))
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(category.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(categoryKind(category), style = MaterialTheme.typography.labelSmall, color = colors.outline)
        }
        IconButton(onClick = onDeleteClick) {
            Icon(MoneyTrailIcons.Trash, contentDescription = "Delete ${category.name}", modifier = Modifier.size(18.dp))
        }
    }
}

private fun categoryKind(category: CustomCategory): String = when (category.type) {
    EntryType.INCOME -> "Income"
    EntryType.EXPENSE -> when (category.spendingType) {
        SpendingType.COMMITTED -> "Expense · committed"
        else -> "Expense · discretionary"
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun SettingsScreenPreview() {
    MoneyTrailTheme {
        SettingsContent(
            uiState = SettingsState(
                isLoading = false,
                hasPlan = true,
                payDay = 25,
                baseSalaryDigits = "120000",
                categories = listOf(
                    CustomCategory("1", "Pet care", EntryType.EXPENSE, SpendingType.COMMITTED),
                    CustomCategory("2", "Tuition classes", EntryType.INCOME, null)
                )
            ),
            onBackClick = {},
            onRetryClick = {},
            onSetUpClick = {},
            onPayDayClick = {},
            onBaseSalaryChange = {},
            onSaveSalaryClick = {},
            onAddCategoryClick = {},
            onAddCategory = { _, _, _ -> },
            onAddCategoryDismiss = {},
            onDeleteCategoryClick = {},
            onDeleteCategoryConfirm = {},
            onDeleteCategoryDismiss = {}
        )
    }
}
