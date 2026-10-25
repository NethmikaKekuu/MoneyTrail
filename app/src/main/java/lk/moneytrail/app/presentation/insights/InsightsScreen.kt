package lk.moneytrail.app.presentation.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.moneytrail.app.domain.model.CategoryTotal
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.MonthTotal
import lk.moneytrail.app.domain.model.SavingsGoal
import lk.moneytrail.app.domain.model.SpendingSplit
import lk.moneytrail.app.domain.model.SpendingType
import lk.moneytrail.app.presentation.components.EmptyView
import lk.moneytrail.app.presentation.components.ErrorView
import lk.moneytrail.app.presentation.components.LoadingView
import lk.moneytrail.app.presentation.components.formatAmount
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// MoneyTrail-Figma-HTML/10-ExpenseInsights.html
@Composable
fun InsightsScreen(
    modifier: Modifier = Modifier,
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    InsightsContent(
        uiState = uiState,
        onTabClick = viewModel::onTabClick,
        onRetryClick = viewModel::onRetryClick,
        modifier = modifier
    )
}

private val ShortMonthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)

private fun entriesText(count: Int): String = if (count == 1) "1 entry" else "$count entries"

@Composable
private fun InsightsContent(
    uiState: InsightsState,
    onTabClick: (InsightsTab) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Insights", style = MaterialTheme.typography.headlineSmall)
            InsightsTabs(selected = uiState.tab, onTabClick = onTabClick)
        }

        when {
            uiState.tab == InsightsTab.INCOME -> EmptyView(
                title = "Income insights are on the way",
                message = "Income by source, and how much of it you can count on, will show here."
            )
            uiState.errorMessage != null -> ErrorView(
                title = "Couldn't load your insights",
                message = uiState.errorMessage,
                onRetry = onRetryClick
            )
            uiState.isLoading -> LoadingView(message = "Loading your insights…")
            else -> ExpenseInsights(uiState)
        }
    }
}

@Composable
private fun InsightsTabs(selected: InsightsTab, onTabClick: (InsightsTab) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow, RoundedCornerShape(16.dp))
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(InsightsTab.EXPENSES to "Expenses", InsightsTab.INCOME to "Income").forEach { (tab, label) ->
            val isSelected = tab == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) colors.primary else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onTabClick(tab) }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 15.sp,
                    color = if (isSelected) colors.onPrimary else colors.outline
                )
            }
        }
    }
}

@Composable
private fun ExpenseInsights(uiState: InsightsState) {
    val colors = MaterialTheme.colorScheme
    val month = uiState.month.format(ShortMonthFormatter)
    val day = uiState.today.dayOfMonth

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (day == 1) "Spent · 1 $month" else "Spent · 1–$day $month",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.outline
                )
                Text(
                    text = "LKR ${formatAmount(uiState.spent.total)}",
                    style = MaterialTheme.typography.displaySmall,
                    fontSize = 32.sp,
                    letterSpacing = (-0.02).em
                )
            }
            Text(
                text = entriesText(uiState.spent.entryCount),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.outline,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        InsightCard(title = "Committed vs discretionary") {
            SplitBar(uiState.split)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SplitValue(
                    label = "Committed · ${uiState.split.committedPercent}%",
                    amount = uiState.split.committed,
                    color = colors.secondary,
                    modifier = Modifier.weight(1f)
                )
                SplitValue(
                    label = "Discretionary · ${uiState.split.discretionaryPercent}%",
                    amount = uiState.split.discretionary,
                    color = colors.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }
            val days = uiState.diningDaysOfSaving
            val goal = uiState.goal
            if (days != null && goal != null) {
                Text(
                    text = "Coffee and dining out so far = about $days ${if (days == 1) "day" else "days"} of saving for your ${goal.name}.",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Normal,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceContainerLow, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }

        InsightCard(title = "By category") {
            if (uiState.categories.isEmpty()) {
                QuietText("Nothing spent yet this month.")
            } else {
                val biggest = uiState.categories.maxOf { it.total }.coerceAtLeast(1)
                uiState.categories.forEach { category ->
                    CategoryBar(category, fraction = category.total.toFloat() / biggest)
                }
            }
        }

        InsightCard(
            title = "Last ${InsightsState.CHART_MONTHS} months",
            note = "$month: LKR ${formatAmount(uiState.spent.total)} so far"
        ) {
            MonthChart(uiState.lastMonths)
        }
    }
}

/** A bordered card with a bold title and an optional note on the right. */
@Composable
private fun InsightCard(title: String, note: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge, fontSize = 14.sp)
            if (note != null) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        content()
    }
}

/** Committed (dark) and discretionary (orange) side by side, each as wide as its share. */
@Composable
private fun SplitBar(split: SpendingSplit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(4.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clearAndSetSemantics {
                contentDescription = "Committed ${formatAmount(split.committed)} (${split.committedPercent}%), " +
                    "discretionary ${formatAmount(split.discretionary)} (${split.discretionaryPercent}%)"
            },
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (split.total == 0L) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(colors.outlineVariant, shape)
            )
        } else {
            if (split.committed > 0) {
                Box(
                    Modifier
                        .weight(split.committed.toFloat())
                        .fillMaxHeight()
                        .background(colors.secondary, shape)
                )
            }
            if (split.discretionary > 0) {
                Box(
                    Modifier
                        .weight(split.discretionary.toFloat())
                        .fillMaxHeight()
                        .background(colors.tertiary, shape)
                )
            }
        }
    }
}

@Composable
private fun SplitValue(label: String, amount: Long, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(formatAmount(amount), style = MaterialTheme.typography.titleSmall, fontSize = 15.sp)
    }
}

@Composable
private fun CategoryBar(category: CategoryTotal, fraction: Float) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 30.dp)
            .clearAndSetSemantics { contentDescription = "${category.category}, LKR ${formatAmount(category.total)}" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = category.category,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(92.dp)
        )
        Box(
            Modifier
                .weight(1f)
                .height(12.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(colors.tertiary, RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
            )
        }
        Text(
            text = formatAmount(category.total),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.width(56.dp)
        )
    }
}

/** One bar per month, as tall as its total against the biggest; this month in full orange. */
@Composable
private fun MonthChart(months: List<MonthTotal>) {
    val colors = MaterialTheme.colorScheme
    val biggest = months.maxOf { it.total }.coerceAtLeast(1)
    val current = months.last().month

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            months.forEach { month ->
                val isCurrent = month.month == current
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics {
                            contentDescription = "${month.month.format(ShortMonthFormatter)} LKR ${formatAmount(month.total)}" +
                                if (isCurrent) " so far" else ""
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        Modifier
                            .widthIn(max = 30.dp)
                            .fillMaxWidth()
                            .fillMaxHeight(month.total.toFloat() / biggest)
                            .background(
                                color = if (isCurrent) colors.tertiary else colors.tertiary.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                            )
                    )
                }
            }
        }
        HorizontalDivider(color = colors.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            months.forEach { month ->
                Text(
                    text = month.month.format(ShortMonthFormatter),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuietText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
}

private fun previewExpense(id: String, amount: Long, category: String, date: LocalDate, type: SpendingType) =
    Entry(id, EntryType.EXPENSE, amount, category, "Main bank", date, "", type)

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun InsightsScreenPreview() {
    val today = LocalDate.of(2026, 10, 10)
    MoneyTrailTheme {
        InsightsContent(
            uiState = InsightsState(
                today = today,
                isLoading = false,
                entries = listOf(
                    previewExpense("1", 34_000, "Rent", today.withDayOfMonth(1), SpendingType.COMMITTED),
                    previewExpense("2", 7_400, "Utilities", today.withDayOfMonth(7), SpendingType.COMMITTED),
                    previewExpense("3", 2_950, "Coffee", today, SpendingType.DISCRETIONARY),
                    previewExpense("4", 2_400, "Food out", today, SpendingType.DISCRETIONARY),
                    previewExpense("5", 154_800, "Rent", LocalDate.of(2026, 9, 1), SpendingType.COMMITTED),
                    previewExpense("6", 172_300, "Rent", LocalDate.of(2026, 8, 1), SpendingType.COMMITTED)
                ),
                goal = SavingsGoal("MacBook Pro M4", 490_000, 11_200, LocalDate.of(2027, 10, 10))
            ),
            onTabClick = {},
            onRetryClick = {}
        )
    }
}
