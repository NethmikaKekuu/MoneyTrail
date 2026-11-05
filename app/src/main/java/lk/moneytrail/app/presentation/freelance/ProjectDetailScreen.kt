package lk.moneytrail.app.presentation.freelance

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import lk.moneytrail.app.domain.model.Project
import lk.moneytrail.app.domain.model.dueDate
import lk.moneytrail.app.domain.model.dueTimestampOf
import lk.moneytrail.app.domain.model.isOverdue
import lk.moneytrail.app.presentation.components.ErrorView
import lk.moneytrail.app.presentation.components.LoadingView
import lk.moneytrail.app.presentation.components.MoneyTrailIcons
import lk.moneytrail.app.presentation.components.formatAmount
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme
import java.time.LocalDate

@Composable
fun ProjectDetailScreen(
    onBackClick: () -> Unit,
    onRecordPaymentClick: (projectId: String, milestoneId: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProjectDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message, uiState.undoMessage) {
        val message = uiState.message
        val undo = uiState.undoMessage
        when {
            message != null -> {
                snackbarHostState.showSnackbar(message)
                viewModel.onMessageShown()
            }
            undo != null -> {
                val result = snackbarHostState.showSnackbar(undo, actionLabel = "Undo", duration = SnackbarDuration.Short)
                if (result == SnackbarResult.ActionPerformed) viewModel.onUndo() else viewModel.onMessageShown()
            }
        }
    }

    val project = uiState.project
    Scaffold(
        modifier = modifier,
        topBar = {
            FreelanceTopBar(
                title = project?.name ?: "Project",
                onBackClick = onBackClick,
                actionIcon = if (project != null) MoneyTrailIcons.Trash else null,
                actionDescription = "Delete project",
                onActionClick = viewModel::onAskDelete
            )
        },
        bottomBar = {
            if (project != null) {
                BottomActions(
                    canAdd = uiState.canAddMilestone,
                    canRecord = uiState.hasUnpaid && !uiState.isSaving,
                    onAdd = viewModel::onOpenAddMilestone,
                    onRecord = { onRecordPaymentClick(project.id, null) }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                uiState.isLoading -> LoadingView(message = "Loading the project…")
                project == null -> ErrorView(
                    message = uiState.loadError ?: "This project no longer exists.",
                    onRetry = viewModel::onRetry
                )
                else -> ProjectDetailContent(
                    state = uiState,
                    project = project,
                    onEdit = viewModel::onEditMilestone,
                    onMarkInvoiced = viewModel::onMarkInvoiced,
                    onUndoInvoiced = viewModel::onUndoInvoiced,
                    onRecordPayment = { milestoneId -> onRecordPaymentClick(project.id, milestoneId) }
                )
            }
        }
    }

    if (uiState.isAddMilestoneOpen) {
        MilestoneDialog(
            heading = "Add milestone",
            confirmLabel = "Add",
            initialTitle = "",
            initialAmount = uiState.leftToSplit.coerceAtLeast(0L),
            initialDueDate = uiState.nextDueDate(),
            maxAmount = uiState.addMaxAmount,
            onSave = viewModel::onAddMilestone,
            onDismiss = viewModel::onDismissAddMilestone
        )
    }

    uiState.editingMilestone?.let { milestone ->
        MilestoneDialog(
            heading = "Edit milestone",
            confirmLabel = "Save",
            initialTitle = milestone.title,
            initialAmount = milestone.amount,
            initialDueDate = milestone.dueDate ?: uiState.nextDueDate(),
            maxAmount = uiState.maxAmountFor(milestone),
            onSave = viewModel::onSaveEditedMilestone,
            onDismiss = viewModel::onDismissEditMilestone,
            onRemove = { viewModel.onRemoveMilestone(milestone) }
        )
    }

    if (uiState.isDeleteConfirmOpen && project != null) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissDelete,
            title = { Text("Delete ${project.name}?", fontWeight = FontWeight.Bold) },
            text = { Text("The project and its milestones are removed. Payments already recorded stay on the calendar.") },
            confirmButton = {
                TextButton(onClick = { viewModel.onConfirmDelete(onDeleted = onBackClick) }) {
                    Text("Delete", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = viewModel::onDismissDelete) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ProjectDetailContent(
    state: ProjectDetailUiState,
    project: Project,
    onEdit: (Milestone) -> Unit,
    onMarkInvoiced: (String) -> Unit,
    onUndoInvoiced: (String) -> Unit,
    onRecordPayment: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ProjectSummaryCard(project, state.today) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Milestones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${project.milestones.size}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (state.milestones.isEmpty()) {
            item {
                Text(
                    "No milestones yet. Add one to invoice the client.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(state.milestones, key = { it.id }) { milestone ->
            MilestoneCard(
                milestone = milestone,
                today = state.today,
                enabled = !state.isSaving,
                onEdit = { onEdit(milestone) },
                onMarkInvoiced = { onMarkInvoiced(milestone.id) },
                onUndoInvoiced = { onUndoInvoiced(milestone.id) },
                onRecordPayment = { onRecordPayment(milestone.id) }
            )
        }
    }
}

@Composable
private fun ProjectSummaryCard(project: Project, today: LocalDate) {
    val overdue = project.overdueMilestones(today)
    Surface(
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Project price", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("LKR ${formatAmount(project.totalBudget)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(project.client, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                val (label, emphasis) = project.statusChip(today)
                StatusChip(label, emphasis)
            }
            ProjectProgressBar(project)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendItem(MaterialTheme.colorScheme.primary, "Paid ${formatAmount(project.paidAmount)}")
                LegendItem(MaterialTheme.colorScheme.tertiary, "Invoiced ${formatAmount(project.invoicedAmount)}")
                LegendItem(MaterialTheme.colorScheme.surfaceVariant, "Planned ${formatAmount(project.plannedAmount)}")
            }
            if (project.unsplitAmount > 0) {
                Text(
                    "${formatAmount(project.unsplitAmount)} of the price isn't in a milestone yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (overdue.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    "${overdue.size} overdue · LKR ${formatAmount(overdue.sumOf { it.amount })}. Follow up with ${project.client}.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun MilestoneCard(
    milestone: Milestone,
    today: LocalDate,
    enabled: Boolean,
    onEdit: () -> Unit,
    onMarkInvoiced: () -> Unit,
    onUndoInvoiced: () -> Unit,
    onRecordPayment: () -> Unit
) {
    val overdue = milestone.isOverdue(today)
    val (chip, emphasis) = when {
        overdue -> "Overdue" to ChipEmphasis.ALERT
        milestone.status == MilestoneStatus.INVOICED -> "Invoiced" to ChipEmphasis.ACTIVE
        milestone.status == MilestoneStatus.PAID -> "Paid" to ChipEmphasis.DONE
        else -> "Planned" to ChipEmphasis.QUIET
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (overdue) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(milestone.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        milestone.dueLabel(today),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(formatAmount(milestone.amount), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    StatusChip(chip, emphasis)
                }
            }

            when (milestone.status) {
                MilestoneStatus.PLANNED -> ActionRow(
                    secondary = "Edit", onSecondary = onEdit,
                    primary = "Mark invoiced", onPrimary = onMarkInvoiced,
                    enabled = enabled
                )
                MilestoneStatus.INVOICED -> ActionRow(
                    secondary = "Not invoiced", onSecondary = onUndoInvoiced,
                    primary = "Record payment", onPrimary = onRecordPayment,
                    enabled = enabled
                )
                MilestoneStatus.PAID -> Text(
                    "Received and on the calendar as income.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Two buttons of the same width and height, secondary on the left. */
@Composable
private fun ActionRow(
    secondary: String,
    onSecondary: () -> Unit,
    primary: String,
    onPrimary: () -> Unit,
    enabled: Boolean,
    height: androidx.compose.ui.unit.Dp = 44.dp
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = onSecondary,
            enabled = enabled,
            shape = RoundedCornerShape(height / 2),
            contentPadding = PaddingValues(horizontal = 12.dp),
            modifier = Modifier.weight(1f).height(height)
        ) { ButtonLabel(secondary) }
        Button(
            onClick = onPrimary,
            enabled = enabled,
            shape = RoundedCornerShape(height / 2),
            contentPadding = PaddingValues(horizontal = 12.dp),
            modifier = Modifier.weight(1f).height(height)
        ) { ButtonLabel(primary) }
    }
}

/** One line, centred, same type in every freelance button. */
@Composable
private fun ButtonLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun BottomActions(canAdd: Boolean, canRecord: Boolean, onAdd: () -> Unit, onRecord: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onAdd,
                    enabled = canAdd,
                    shape = RoundedCornerShape(26.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.weight(1f).height(52.dp)
                ) { ButtonLabel("Add milestone") }
                Button(
                    onClick = onRecord,
                    enabled = canRecord,
                    shape = RoundedCornerShape(26.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.weight(1f).height(52.dp)
                ) { ButtonLabel("Record payment") }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ProjectDetailPreview() {
    val today = LocalDate.of(2026, 11, 5)
    val project = Project(
        "p1", "Bakery website", "Sweet Crumbs", 120_000L,
        listOf(
            Milestone("m1", "Design", 30_000L, MilestoneStatus.PAID, dueTimestampOf(today.minusDays(40))),
            Milestone("m2", "Launch", 60_000L, MilestoneStatus.INVOICED, dueTimestampOf(today.minusDays(3))),
            Milestone("m3", "Support", 20_000L, MilestoneStatus.PLANNED, dueTimestampOf(today.plusDays(20)))
        )
    )
    MoneyTrailTheme {
        ProjectDetailContent(
            state = ProjectDetailUiState(project = project, today = today, isLoading = false),
            project = project,
            onEdit = {}, onMarkInvoiced = {}, onUndoInvoiced = {}, onRecordPayment = {}
        )
    }
}
