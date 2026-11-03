package lk.moneytrail.app.presentation.settings

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.moneytrail.app.data.preferences.ThemeMode
import lk.moneytrail.app.domain.model.CustomCategory
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.SpendingType
import lk.moneytrail.app.presentation.auth.AuthBackButton
import lk.moneytrail.app.presentation.auth.AuthErrorText
import lk.moneytrail.app.presentation.components.AddCategoryDialog
import lk.moneytrail.app.presentation.components.MoneyTrailIcons
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme

// Not in the Figma file; follows the style of the onboarding and edit entry screens.
// The app theme and the user's own categories. The salary and pay day are on Salary.
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onThemeModeClick = viewModel::onThemeModeClick,
        onCheckRemindersClick = viewModel::onCheckRemindersClick,
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
    onThemeModeClick: (ThemeMode) -> Unit,
    onCheckRemindersClick: () -> Unit,
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

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionTitle("Theme")
            ThemeSection(selected = uiState.themeMode, onThemeModeClick = onThemeModeClick)

            Spacer(Modifier.height(14.dp))
            SectionTitle("Reminders")
            RemindersSection(onCheckRemindersClick = onCheckRemindersClick)

            Spacer(Modifier.height(14.dp))
            SectionTitle("Your categories")
            CategoriesSection(uiState, onAddCategoryClick, onDeleteCategoryClick)
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

/** Light, Dark or the phone's own setting, as radio rows. */
@Composable
private fun ThemeSection(selected: ThemeMode, onThemeModeClick: (ThemeMode) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow, RoundedCornerShape(16.dp))
            .selectableGroup()
    ) {
        ThemeMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .selectable(selected = mode == selected, role = Role.RadioButton, onClick = { onThemeModeClick(mode) })
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RadioButton(selected = mode == selected, onClick = null)
                Text(mode.label, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/**
 * When the daily reminders come, whether the phone lets them show, and a way to see them
 * now (for checking, and the demo) instead of waiting for the morning.
 */
@Composable
private fun RemindersSection(onCheckRemindersClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    var areNotificationsOn by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    // Read again on coming back from the phone's settings.
    LifecycleResumeEffect(Unit) {
        areNotificationsOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
        onPauseOrDispose {}
    }

    Text(
        text = "Every morning at 9:00 MoneyTrail sends a notification, even when the app is closed: money due " +
            "that day, paying yourself first once the salary is in, and catching up after a few days away.",
        style = MaterialTheme.typography.bodySmall,
        color = colors.outline
    )
    if (!areNotificationsOn) {
        Text(
            text = "Notifications are off for MoneyTrail.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.error
        )
        TextButton(onClick = {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            )
        }) { Text("Turn on notifications") }
    }
    OutlinedButton(
        onClick = onCheckRemindersClick,
        enabled = areNotificationsOn,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, colors.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Text("Check reminders now", style = MaterialTheme.typography.labelLarge)
    }
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
                themeMode = ThemeMode.SYSTEM,
                categories = listOf(
                    CustomCategory("1", "Pet care", EntryType.EXPENSE, SpendingType.COMMITTED),
                    CustomCategory("2", "Tuition classes", EntryType.INCOME, null)
                )
            ),
            onBackClick = {},
            onThemeModeClick = {},
            onCheckRemindersClick = {},
            onAddCategoryClick = {},
            onAddCategory = { _, _, _ -> },
            onAddCategoryDismiss = {},
            onDeleteCategoryClick = {},
            onDeleteCategoryConfirm = {},
            onDeleteCategoryDismiss = {}
        )
    }
}
