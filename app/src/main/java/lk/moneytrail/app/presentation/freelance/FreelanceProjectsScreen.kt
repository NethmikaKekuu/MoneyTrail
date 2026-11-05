package lk.moneytrail.app.presentation.freelance

import lk.moneytrail.app.presentation.components.ChipEmphasis
import lk.moneytrail.app.presentation.components.LegendItem
import lk.moneytrail.app.presentation.components.ScreenTopBar
import lk.moneytrail.app.presentation.components.StatusChip
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.moneytrail.app.domain.model.Milestone
import lk.moneytrail.app.domain.model.MilestoneStatus
import lk.moneytrail.app.domain.model.OverdueInvoice
import lk.moneytrail.app.domain.model.Project
import lk.moneytrail.app.domain.model.dueTimestampOf
import lk.moneytrail.app.presentation.components.EmptyView
import lk.moneytrail.app.presentation.components.ErrorView
import lk.moneytrail.app.presentation.components.LoadingView
import lk.moneytrail.app.presentation.components.MoneyTrailIcons
import lk.moneytrail.app.presentation.components.formatAmount
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme
import java.time.LocalDate

@Composable
fun FreelanceProjectsScreen(
    onBackClick: () -> Unit,
    onAddProjectClick: () -> Unit,
    onProjectClick: (projectId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FreelanceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            ScreenTopBar(
                title = "Freelance",
                onBackClick = onBackClick,
                actionIcon = MoneyTrailIcons.Plus,
                actionDescription = "New project",
                onActionClick = onAddProjectClick
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                uiState.isLoading -> LoadingView(message = "Loading freelance projects…")
                uiState.errorMessage != null && uiState.projects.isEmpty() -> ErrorView(
                    message = uiState.errorMessage ?: "Couldn't load freelance projects.",
                    onRetry = viewModel::onRetry
                )
                uiState.projects.isEmpty() -> EmptyView(
                    title = "No freelance projects",
                    message = "Add a project with its price, split it into milestones, and track what's invoiced and paid.",
                    actionLabel = "New project",
                    onAction = onAddProjectClick
                )
                else -> FreelanceList(state = uiState, onProjectClick = onProjectClick)
            }
        }
    }
}

@Composable
private fun FreelanceList(
    state: FreelanceUiState,
    onProjectClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { TotalsCard(state) }

        if (state.overdueInvoices.isNotEmpty()) {
            item { SectionTitle("Overdue", "${state.overdueCount}") }
            item { OverdueCard(state.overdueInvoices, state.today, onProjectClick) }
        }

        item { SectionTitle("Projects", "${state.projects.size}") }
        items(state.sortedProjects, key = { it.id }) { project ->
            ProjectCard(project = project, today = state.today, onClick = { onProjectClick(project.id) })
        }
    }
}

@Composable
private fun TotalsCard(state: FreelanceUiState) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Outstanding (invoiced, unpaid)", style = MaterialTheme.typography.bodyMedium, color = onPrimary.copy(alpha = 0.85f))
                Text(
                    "LKR ${formatAmount(state.totalOutstandingInvoiced)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = onPrimary
                )
            }
            HorizontalDivider(color = onPrimary.copy(alpha = 0.2f))
            Row(Modifier.fillMaxWidth()) {
                TotalStat("Overdue", formatAmount(state.overdueAmount), "${state.overdueCount} invoice${if (state.overdueCount == 1) "" else "s"}", Modifier.weight(1f))
                TotalStat("Paid", formatAmount(state.totalPaid), "received", Modifier.weight(1f))
                TotalStat("To invoice", formatAmount(state.totalToInvoice), "planned", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TotalStat(label: String, value: String, caption: String, modifier: Modifier) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = onPrimary.copy(alpha = 0.8f))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = onPrimary, maxLines = 1)
        Text(caption, style = MaterialTheme.typography.bodySmall, color = onPrimary.copy(alpha = 0.7f), maxLines = 1)
    }
}

@Composable
private fun SectionTitle(title: String, count: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(count, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OverdueCard(overdue: List<OverdueInvoice>, today: LocalDate, onProjectClick: (String) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            overdue.forEachIndexed { index, invoice ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Surface(onClick = { onProjectClick(invoice.projectId) }, color = MaterialTheme.colorScheme.surface) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(invoice.milestone, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(invoice.project, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(formatAmount(invoice.amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(invoice.lateText(today), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(project: Project, today: LocalDate, onClick: () -> Unit) {
    val (chipLabel, chipEmphasis) = project.statusChip(today)
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(project.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(project.client, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                StatusChip(chipLabel, chipEmphasis)
                androidx.compose.foundation.layout.Spacer(Modifier.width(4.dp))
                Icon(MoneyTrailIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ProjectProgressBar(project)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(project.paidOfPriceText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (project.invoicedAmount > 0) {
                    Text("Outstanding ${formatAmount(project.invoicedAmount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun FreelanceListPreview() {
    val today = LocalDate.of(2026, 11, 5)
    MoneyTrailTheme {
        FreelanceList(
            state = FreelanceUiState(
                today = today,
                projects = listOf(
                    Project(
                        "p1", "Bakery website", "Sweet Crumbs", 120_000L,
                        listOf(
                            Milestone("m1", "Design", 30_000L, MilestoneStatus.PAID, dueTimestampOf(today.minusDays(40))),
                            Milestone("m2", "Launch", 60_000L, MilestoneStatus.INVOICED, dueTimestampOf(today.minusDays(3))),
                            Milestone("m3", "Support", 30_000L, MilestoneStatus.PLANNED, dueTimestampOf(today.plusDays(20)))
                        )
                    ),
                    Project("p2", "Clinic booking", "Dr. Perera", 80_000L, listOf(Milestone("m4", "Full payment", 80_000L)))
                )
            ),
            onProjectClick = {}
        )
    }
}
