package lk.moneytrail.app.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.SpendingType
import lk.moneytrail.app.presentation.components.AddEntryButton
import lk.moneytrail.app.presentation.components.EmptyView
import lk.moneytrail.app.presentation.components.ErrorView
import lk.moneytrail.app.presentation.components.LoadingView
import lk.moneytrail.app.presentation.components.MoneyTrailIcons
import lk.moneytrail.app.presentation.components.formatNet
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

// MoneyTrail-Figma-HTML/07-Main.html
@Composable
fun CalendarScreen(
    onAddEntryClick: (date: LocalDate) -> Unit,
    onEntryClick: (entryId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val checkActions = remember(viewModel) {
        ExpectedCheckActions(
            onAmountChange = viewModel::onCheckAmountChange,
            onBonusChange = viewModel::onCheckBonusChange,
            onDateChange = viewModel::onCheckDateChange,
            onConfirm = viewModel::onCheckConfirm,
            onNotYet = viewModel::onCheckNotYetClick,
            onReschedule = viewModel::onCheckReschedule,
            onNewDateDismiss = viewModel::onCheckNewDateDismiss,
            onRemove = viewModel::onCheckRemove,
            onDismiss = viewModel::onCheckDismiss
        )
    }

    CalendarContent(
        uiState = uiState,
        onPreviousMonthClick = viewModel::onPreviousMonthClick,
        onNextMonthClick = viewModel::onNextMonthClick,
        onDayClick = viewModel::onDayClick,
        onRetryClick = viewModel::onRetryClick,
        onAddEntryClick = { onAddEntryClick(uiState.selectedDate) },
        onEntryClick = onEntryClick,
        onCheckDueClick = viewModel::onCheckDueClick,
        checkActions = checkActions,
        modifier = modifier
    )
}

private val MonthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
private val DayFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)

@Composable
private fun CalendarContent(
    uiState: CalendarState,
    onPreviousMonthClick: () -> Unit,
    onNextMonthClick: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onRetryClick: () -> Unit,
    onAddEntryClick: () -> Unit,
    onEntryClick: (String) -> Unit,
    onCheckDueClick: () -> Unit,
    checkActions: ExpectedCheckActions,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Hi ${uiState.firstName ?: "there"}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(uiState.month.format(MonthFormatter), style = MaterialTheme.typography.headlineSmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SquareIconButton(MoneyTrailIcons.ChevronLeft, "Previous month", onPreviousMonthClick)
                    SquareIconButton(MoneyTrailIcons.ChevronRight, "Next month", onNextMonthClick)
                }
            }

            if (uiState.dueExpected.isNotEmpty()) {
                DueBanner(
                    due = uiState.dueExpected,
                    today = uiState.today,
                    onCheckClick = onCheckDueClick,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp)
                )
            }

            MonthSummary(uiState.monthTotals, Modifier.padding(horizontal = 20.dp))

            MonthGrid(
                days = uiState.days,
                selectedDate = uiState.selectedDate,
                today = uiState.today,
                onDayClick = onDayClick,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp)
            )

            SelectedDayPanel(
                uiState = uiState,
                onRetryClick = onRetryClick,
                onEntryClick = onEntryClick,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 8.dp)
            )
        }

        AddEntryButton(
            onClick = onAddEntryClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 22.dp, bottom = 16.dp)
        )
    }

    if (uiState.check != null) {
        ExpectedCheckDialog(check = uiState.check, today = uiState.today, actions = checkActions)
    }
}

/** The cream sheet under the grid listing the selected day's entries. */
@Composable
private fun SelectedDayPanel(
    uiState: CalendarState,
    onRetryClick: () -> Unit,
    onEntryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val date = uiState.selectedDate
    val title = if (date == uiState.today) "Today · ${date.format(DayFormatter)}" else date.format(DayFormatter)
    val entries = uiState.selectedDayEntries

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = "LKR ${formatNet(uiState.selectedDayTotals.net)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.outline
            )
        }

        when {
            uiState.errorMessage != null -> ErrorView(
                title = "Couldn't load your entries",
                message = uiState.errorMessage,
                onRetry = onRetryClick
            )
            uiState.isLoading -> LoadingView(message = "Loading your entries…")
            entries.isEmpty() -> EmptyView(
                title = "Nothing logged on this day",
                message = "Tap + to add an expense or income."
            )
            else -> LazyColumn(
                // Bottom space so the + button never covers the last entry.
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    EntryCard(entry, onClick = { onEntryClick(entry.id) })
                }
            }
        }
    }
}

private val PreviewDay = LocalDate.of(2026, 10, 10)

private val PreviewEntries = listOf(
    Entry("1", EntryType.INCOME, 120_000, "Salary", "Main bank", LocalDate.of(2026, 10, 5), "", null),
    Entry("2", EntryType.EXPENSE, 850, "Coffee", "Cash wallet", PreviewDay, "Flat white before stand-up", SpendingType.DISCRETIONARY),
    Entry("3", EntryType.EXPENSE, 2_400, "Food out", "Main bank", PreviewDay, "Farewell lunch for a teammate", SpendingType.DISCRETIONARY),
    Entry("4", EntryType.EXPENSE, 780, "Rides", "Cash wallet", PreviewDay, "", SpendingType.DISCRETIONARY)
)

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun CalendarScreenPreview() {
    MoneyTrailTheme {
        CalendarContent(
            uiState = CalendarState(
                firstName = "Kavindu",
                month = YearMonth.of(2026, 10),
                today = PreviewDay,
                selectedDate = PreviewDay,
                isLoading = false,
                entries = PreviewEntries
            ),
            onPreviousMonthClick = {},
            onNextMonthClick = {},
            onDayClick = {},
            onRetryClick = {},
            onAddEntryClick = {},
            onEntryClick = {},
            onCheckDueClick = {},
            checkActions = ExpectedCheckActions({}, {}, {}, {}, {}, {}, {}, {}, {})
        )
    }
}
