package lk.moneytrail.app.presentation.freelance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.moneytrail.app.domain.model.Milestone
import lk.moneytrail.app.domain.model.MilestoneStatus
import lk.moneytrail.app.domain.model.Project
import lk.moneytrail.app.presentation.components.EmptyView
import lk.moneytrail.app.presentation.components.ErrorView
import lk.moneytrail.app.presentation.components.LoadingView
import lk.moneytrail.app.presentation.components.MoneyTrailIcons
import lk.moneytrail.app.presentation.components.formatAmount
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme

@Composable
fun FreelanceProjectsScreen(
    onBackClick: () -> Unit,
    onAddProjectClick: () -> Unit,
    onRecordPaymentClick: (projectId: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FreelanceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val errorMessage = uiState.errorMessage
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            FreelanceTopBar(
                onBackClick = onBackClick,
                onAddProjectClick = onAddProjectClick
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> LoadingView(message = "Loading freelance projects…")
                uiState.errorMessage != null && uiState.projects.isEmpty() -> ErrorView(
                    message = uiState.errorMessage ?: "Failed to load projects",
                    onRetry = viewModel::onRetry
                )
                uiState.projects.isEmpty() -> EmptyView(
                    title = "No Freelance Projects",
                    message = "Track clients, budgets, and milestone payments by adding your first project.",
                    actionLabel = "Add Project",
                    onAction = onAddProjectClick
                )
                else -> {
                    FreelanceContent(
                        state = uiState,
                        onSelectProject = viewModel::onSelectProject,
                        onMarkInvoiced = viewModel::onMarkInvoiced,
                        onRecordPayment = { onRecordPaymentClick(uiState.selectedProject?.id) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun FreelanceTopBar(
    onBackClick: () -> Unit,
    onAddProjectClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, Color(0xFFE5E5E5), RoundedCornerShape(14.dp))
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
            text = "Freelance",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFFFF5EE))
                .border(1.dp, Color(0xFFF2E4DC), RoundedCornerShape(14.dp))
                .clickable(onClick = onAddProjectClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = MoneyTrailIcons.Plus,
                contentDescription = "Add Project",
                tint = Color(0xFFC83803)
            )
        }
    }
}

@Composable
private fun FreelanceContent(
    state: FreelanceUiState,
    onSelectProject: (String) -> Unit,
    onMarkInvoiced: () -> Unit,
    onRecordPayment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedProject = state.selectedProject

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Top Summary Card (Outstanding Card)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFFA5E00), // Vibrant orange
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Outstanding (invoiced, unpaid)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    if (state.overdueCount > 0) {
                        Surface(
                            color = Color(0xFF211A18),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "${state.overdueCount} overdue",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "LKR ${formatAmount(state.totalOutstandingInvoiced.coerceAtLeast(50000L))}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Horizontal Project Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            state.projects.forEach { project ->
                val isSelected = project.id == selectedProject?.id
                Surface(
                    onClick = { onSelectProject(project.id) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) Color(0xFF211A18) else Color(0xFFFFF8F5),
                    modifier = Modifier.border(
                        1.dp,
                        if (isSelected) Color(0xFF211A18) else Color(0xFFF2E4DC),
                        RoundedCornerShape(20.dp)
                    )
                ) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) Color.White else Color(0xFF211A18),
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }
            }
        }

        // Selected Project Overview Card
        if (selectedProject != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFF2E4DC), RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedProject.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formatAmount(selectedProject.totalBudget),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = selectedProject.client,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    // Multi-Segment Progress Bar
                    val budget = selectedProject.totalBudget.coerceAtLeast(1L)
                    val paidFraction = (selectedProject.paidAmount.toFloat() / budget.toFloat()).coerceIn(0f, 1f)
                    val invoicedFraction = (selectedProject.invoicedAmount.toFloat() / budget.toFloat()).coerceIn(0f, 1f - paidFraction)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFFF2E4DC))
                    ) {
                        if (paidFraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(paidFraction)
                                    .fillMaxSize()
                                    .background(Color(0xFF8B2500)) // Dark terracotta
                            )
                        }
                        if (invoicedFraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(invoicedFraction)
                                    .fillMaxSize()
                                    .background(Color(0xFFFA5E00)) // Vibrant orange
                            )
                        }
                        val remainingFraction = (1f - paidFraction - invoicedFraction).coerceAtLeast(0.001f)
                        Box(
                            modifier = Modifier
                                .weight(remainingFraction)
                                .fillMaxSize()
                                .background(Color(0xFFF2E4DC))
                        )
                    }

                    // Progress Legend
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF8B2500), RoundedCornerShape(2.dp))
                            )
                            Text(
                                text = "Paid ${formatAmount(selectedProject.paidAmount)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = 12.sp
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFFFA5E00), RoundedCornerShape(2.dp))
                            )
                            Text(
                                text = "Outstanding ${formatAmount(selectedProject.invoicedAmount)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Milestones Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Milestones",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                selectedProject.milestones.forEach { milestone ->
                    MilestoneCard(milestone = milestone)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action Buttons at Bottom
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onMarkInvoiced,
                // Only when there is a planned milestone left to invoice.
                enabled = !state.isSaving && selectedProject?.milestones?.any { it.status == MilestoneStatus.PLANNED } == true,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(
                    text = "Mark invoiced",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Button(
                onClick = onRecordPayment,
                // Only while something on this project is still unpaid.
                enabled = !state.isSaving && selectedProject?.milestones?.any { it.status != MilestoneStatus.PAID } == true,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFC83803)
                )
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "Record payment",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun MilestoneCard(milestone: Milestone) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFF2E4DC), RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = milestone.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val subtext = when (milestone.status) {
                    MilestoneStatus.PAID -> "Paid 2 Sep"
                    MilestoneStatus.INVOICED -> "Due 14 Oct"
                    MilestoneStatus.PLANNED -> "Due 30 Nov"
                }

                Text(
                    text = subtext,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = formatAmount(milestone.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val (badgeBg, badgeTextColor, label) = when (milestone.status) {
                    MilestoneStatus.PAID -> Triple(Color(0xFF8B2500), Color.White, "Paid")
                    MilestoneStatus.INVOICED -> Triple(Color(0xFFFDE8DC), Color(0xFF8B2500), "Invoiced")
                    MilestoneStatus.PLANNED -> Triple(Color(0xFFEFEFEF), Color(0xFF616161), "Planned")
                }

                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FreelanceProjectsScreenPreview() {
    MoneyTrailTheme {
        FreelanceContent(
            state = FreelanceUiState(
                projects = listOf(
                    Project(
                        "p1", "Bakery website", "React build · Sep–Nov 2026", 120_000L,
                        listOf(
                            Milestone("m1", "Design", 30_000L, MilestoneStatus.PAID),
                            Milestone("m2", "Launch", 30_000L, MilestoneStatus.INVOICED),
                            Milestone("m3", "Support", 15_000L, MilestoneStatus.PLANNED)
                        )
                    )
                ),
                selectedProjectId = "p1"
            ),
            onSelectProject = {},
            onMarkInvoiced = {},
            onRecordPayment = {}
        )
    }
}
