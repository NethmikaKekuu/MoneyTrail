package lk.moneytrail.app.presentation.folders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.moneytrail.app.domain.model.CategoryTotal
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.MonthTotal
import lk.moneytrail.app.domain.model.SpendingType
import lk.moneytrail.app.presentation.components.AddEntryButton
import lk.moneytrail.app.presentation.components.ErrorView
import lk.moneytrail.app.presentation.components.LoadingView
import lk.moneytrail.app.presentation.components.MoneyTrailIcons
import lk.moneytrail.app.presentation.components.formatAmount
import lk.moneytrail.app.presentation.theme.Ink
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

// MoneyTrail-Figma-HTML/09-Folders.html
@Composable
fun FoldersScreen(
    onAddEntryClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FoldersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FoldersContent(
        uiState = uiState,
        onFolderClick = viewModel::onFolderClick,
        onMonthClick = viewModel::onMonthClick,
        onRetryClick = viewModel::onRetryClick,
        onAddEntryClick = onAddEntryClick,
        modifier = modifier
    )
}

private val MonthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
private val ShortMonthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)

private fun entriesText(count: Int): String = if (count == 1) "1 entry" else "$count entries"

private fun folderName(type: EntryType): String = if (type == EntryType.EXPENSE) "Expenses" else "Income"

@Composable
private fun FoldersContent(
    uiState: FoldersState,
    onFolderClick: (EntryType) -> Unit,
    onMonthClick: (YearMonth) -> Unit,
    onRetryClick: () -> Unit,
    onAddEntryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Box(modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Folders", style = MaterialTheme.typography.headlineSmall)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(MoneyTrailIcons.Calendar, contentDescription = null, tint = colors.outline, modifier = Modifier.size(14.dp))
                    Text("Auto-updated from your calendar", style = MaterialTheme.typography.bodySmall, color = colors.outline)
                }
            }

            Row(
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(EntryType.EXPENSE to uiState.expenses, EntryType.INCOME to uiState.income).forEach { (type, total) ->
                    FolderCard(
                        name = folderName(type),
                        total = total,
                        selected = type == uiState.selectedType,
                        onClick = { onFolderClick(type) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            when {
                uiState.errorMessage != null -> ErrorView(
                    title = "Couldn't load your folders",
                    message = uiState.errorMessage,
                    onRetry = onRetryClick
                )
                uiState.isLoading -> LoadingView(message = "Loading your folders…")
                else -> LazyColumn(
                    // Bottom space so the + button never covers the last row.
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "${folderName(uiState.selectedType)} › ${uiState.selectedMonth.format(MonthYearFormatter)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    item {
                        if (uiState.categories.isEmpty()) {
                            EmptyFolder(uiState.selectedType, uiState.selectedMonth)
                        } else {
                            CategoryList(uiState.categories)
                        }
                    }
                    if (uiState.otherMonths.isNotEmpty()) {
                        item {
                            Text(
                                text = if (uiState.selectedMonth == uiState.currentMonth) "Earlier months" else "Other months",
                                style = MaterialTheme.typography.titleSmall,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        items(uiState.otherMonths, key = { it.month.toString() }) { month ->
                            MonthRow(month, onClick = { onMonthClick(month.month) })
                        }
                    }
                }
            }
        }

        AddEntryButton(
            onClick = onAddEntryClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 22.dp, bottom = 16.dp)
        )
    }
}

/** The Expenses or Income card; the picked one is orange. */
@Composable
private fun FolderCard(
    name: String,
    total: MonthTotal,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    // The picked card is always orange, so its text stays dark in both themes.
    val textColor = if (selected) Ink else colors.onSurface
    Column(
        modifier = modifier
            .heightIn(min = 128.dp)
            .clip(shape)
            .background(if (selected) colors.tertiary else colors.surface)
            .border(1.5.dp, if (selected) colors.tertiary else colors.outlineVariant, shape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = MoneyTrailIcons.Folder,
            contentDescription = null,
            tint = if (selected) Ink else colors.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(name, style = MaterialTheme.typography.labelLarge, fontSize = 14.sp, color = textColor)
        Text(formatAmount(total.total), style = MaterialTheme.typography.titleMedium, fontSize = 18.sp, color = textColor)
        Text(
            text = "${entriesText(total.entryCount)} in ${total.month.format(ShortMonthFormatter)}",
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Ink else colors.outline
        )
    }
}

/** The open folder's categories, each with a bar against the biggest one. */
@Composable
private fun CategoryList(categories: List<CategoryTotal>) {
    val colors = MaterialTheme.colorScheme
    val biggest = categories.maxOf { it.total }.coerceAtLeast(1)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        categories.forEachIndexed { index, category ->
            CategoryRow(category, fraction = category.total.toFloat() / biggest)
            if (index < categories.lastIndex) {
                HorizontalDivider(color = colors.outlineVariant)
            }
        }
    }
}

@Composable
private fun CategoryRow(category: CategoryTotal, fraction: Float) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(colors.primaryContainer, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = category.category.take(1).uppercase(),
                style = MaterialTheme.typography.titleSmall,
                fontSize = 15.sp,
                color = colors.primary
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(category.category, style = MaterialTheme.typography.bodyMedium, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(formatAmount(category.total), style = MaterialTheme.typography.titleSmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(6.dp)
                        .background(colors.primaryContainer, RoundedCornerShape(3.dp))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .background(colors.tertiary, RoundedCornerShape(3.dp))
                    )
                }
                Text(
                    text = entriesText(category.entryCount),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Normal,
                    color = colors.outline
                )
            }
        }
    }
}

@Composable
private fun EmptyFolder(type: EntryType, month: YearMonth) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "No ${folderName(type).lowercase()} in ${month.format(MonthYearFormatter)} yet",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Tap + to add one, or open another month below.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

/** A month to switch the folders to. */
@Composable
private fun MonthRow(month: MonthTotal, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 56.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(MoneyTrailIcons.Folder, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
            Text(
                text = month.month.format(MonthYearFormatter),
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(entriesText(month.entryCount), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Normal, color = colors.outline)
            Text(formatAmount(month.total), style = MaterialTheme.typography.titleSmall)
        }
    }
}

private fun previewEntry(id: String, amount: Long, category: String, date: LocalDate) =
    Entry(id, EntryType.EXPENSE, amount, category, "Main bank", date, "", SpendingType.DISCRETIONARY)

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun FoldersScreenPreview() {
    val october = LocalDate.of(2026, 10, 5)
    MoneyTrailTheme {
        FoldersContent(
            uiState = FoldersState(
                currentMonth = YearMonth.of(2026, 10),
                isLoading = false,
                entries = listOf(
                    previewEntry("1", 34_000, "Rent", october),
                    previewEntry("2", 6_850, "Groceries", october),
                    previewEntry("3", 850, "Coffee", october),
                    previewEntry("4", 2_100, "Coffee", october),
                    previewEntry("5", 1_730, "Rides", october),
                    previewEntry("6", 41_000, "Rent", LocalDate.of(2026, 9, 1))
                )
            ),
            onFolderClick = {},
            onMonthClick = {},
            onRetryClick = {},
            onAddEntryClick = {}
        )
    }
}
