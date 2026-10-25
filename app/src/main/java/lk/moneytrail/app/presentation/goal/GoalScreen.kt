package lk.moneytrail.app.presentation.goal

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.moneytrail.app.domain.model.GoalModel
import lk.moneytrail.app.presentation.components.EmptyView
import lk.moneytrail.app.presentation.components.ErrorView
import lk.moneytrail.app.presentation.components.LoadingView
import lk.moneytrail.app.presentation.components.MoneyTrailIcons
import lk.moneytrail.app.presentation.components.formatAmount
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// One colour per goal, so several goals are easy to tell apart on light and dark backgrounds.
private val GoalColors = listOf(
    Color(0xFFD84315), // Terracotta Orange
    Color(0xFF00897B), // Deep Teal
    Color(0xFF3F51B5), // Indigo
    Color(0xFF7B1FA2), // Purple
    Color(0xFF689F38)  // Olive Green
)

@Composable
fun GoalScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GoalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val bannerMessage = (uiState as? GoalUiState.Success)?.bannerMessage
    LaunchedEffect(bannerMessage) {
        if (bannerMessage != null) {
            snackbarHostState.showSnackbar(bannerMessage)
            viewModel.onDismissBanner()
        }
    }

    // "Remind me tomorrow" is saved: leave, and the calendar asks again tomorrow.
    val isReminderSet = (uiState as? GoalUiState.Success)?.isReminderSet == true
    LaunchedEffect(isReminderSet) {
        if (isReminderSet) {
            viewModel.onReminderHandled()
            onBackClick()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            GoalTopBar(
                onBackClick = onBackClick,
                onOpenNewGoalDialog = viewModel::onOpenNewGoalDialog
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
            when (val state = uiState) {
                GoalUiState.Loading -> LoadingView(message = "Loading goal progress…")
                // Goals live in the money plan, so without one there's nowhere to add a goal yet.
                GoalUiState.Empty -> EmptyView(
                    title = "No Savings Goal Set",
                    message = "Set up your salary and goal from More › Confirm salary, and your progress will show here."
                )
                is GoalUiState.Error -> ErrorView(
                    message = state.message,
                    onRetry = viewModel::onRetry
                )
                is GoalUiState.Success -> {
                    GoalContent(
                        state = state,
                        onMovedItClick = viewModel::onOpenAllocationDialog,
                        onRemindTomorrowClick = viewModel::onRemindTomorrowClick,
                        onOpenNewGoalDialog = viewModel::onOpenNewGoalDialog,
                        onEditGoal = viewModel::onEditGoalClick,
                        onDeleteGoal = viewModel::onDeleteGoalClick,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (state.isNewGoalDialogOpen) {
                        // The key gives the dialog fresh fields for each goal it opens with.
                        key(state.editingGoal?.id) {
                            NewGoalDialog(
                                defaultTransferDay = state.payDay,
                                initialGoal = state.editingGoal,
                                onSaveGoal = viewModel::onSaveNewGoal,
                                onDismiss = viewModel::onDismissNewGoalDialog
                            )
                        }
                    }

                    if (state.deletingGoalId != null) {
                        val name = state.goals.find { it.id == state.deletingGoalId }?.name.orEmpty()
                        AlertDialog(
                            onDismissRequest = viewModel::onDeleteGoalDismiss,
                            title = { Text("Remove $name?") },
                            text = { Text("Its progress goes too. Money already in your savings account isn't touched.") },
                            confirmButton = {
                                TextButton(onClick = viewModel::onDeleteGoalConfirm) {
                                    Text("Remove", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = viewModel::onDeleteGoalDismiss) { Text("Keep") }
                            }
                        )
                    }

                    if (state.isAllocationDialogOpen) {
                        GoalAllocationDialog(
                            totalPoolAmount = state.totalAllocationPool,
                            goals = state.goals,
                            isSubmitting = state.isTransferring,
                            onConfirmAllocation = viewModel::onConfirmSalaryAllocation,
                            onDismiss = viewModel::onDismissAllocationDialog
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SalaryConfirmedBanner(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = MoneyTrailIcons.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
            val textColor = MaterialTheme.colorScheme.onPrimaryContainer
            val annotatedText = buildAnnotatedString {
                if (text.contains("Salary confirmed:")) {
                    append("Salary confirmed: ")
                    val remaining = text.substringAfter("Salary confirmed: ")
                    val amountPart = remaining.substringBefore(" (")
                    val bonusPart = if (remaining.contains(" (")) " (" + remaining.substringAfter(" (") else ""
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = textColor)) {
                        append(amountPart)
                    }
                    if (bonusPart.isNotEmpty()) {
                        append(bonusPart)
                    }
                } else {
                    append(text)
                }
            }
            Text(
                text = annotatedText,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }
    }
}

@Composable
private fun GoalTopBar(
    onBackClick: () -> Unit,
    onOpenNewGoalDialog: () -> Unit
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
            text = "Pay yourself first",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                .clickable(onClick = onOpenNewGoalDialog),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = MoneyTrailIcons.Plus,
                contentDescription = "New Goal",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun GoalContent(
    state: GoalUiState.Success,
    onMovedItClick: () -> Unit,
    onRemindTomorrowClick: () -> Unit,
    onOpenNewGoalDialog: () -> Unit,
    onEditGoal: (String) -> Unit,
    onDeleteGoal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(top = 12.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Shown once this month's salary has been confirmed
        state.salaryConfirmedText?.let { SalaryConfirmedBanner(text = it) }

        // One progress card for each goal
        state.goals.forEachIndexed { index, goal ->
            val colorIndex = index % GoalColors.size
            SingleGoalCard(
                goal = goal,
                progressColor = GoalColors[colorIndex],
                trackColor = GoalColors[colorIndex].copy(alpha = 0.18f),
                onEdit = { onEditGoal(goal.id) },
                onDelete = { onDeleteGoal(goal.id) }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when {
                state.isSavingsToDo -> {
                    MoveSavingsCard(amount = state.totalAllocationPool, fromAccount = state.fromAccount)

                    Button(
                        onClick = onMovedItClick,
                        enabled = !state.isTransferring,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        if (state.isTransferring) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(text = "I’ve moved it", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    TextButton(onClick = onRemindTomorrowClick, enabled = !state.isTransferring) {
                        Text(
                            text = "Remind me tomorrow",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                state.areAllGoalsReached -> StatusCard(
                    title = "Every goal is reached",
                    message = "Well done. Add a new goal to keep paying yourself first."
                )
                state.isSavingsMovedThisMonth -> StatusCard(
                    title = "Done for ${LocalDate.now().format(MonthFormatter)}",
                    message = state.nextSavingsDate
                        ?.let { "Next saving is due on ${it.format(DayFormatter)}, once your salary is in." }
                        ?: "Next saving is due next month."
                )
            }

            OutlinedButton(
                onClick = onOpenNewGoalDialog,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "+ Add Another Savings Goal",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private val MonthFormatter = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)
private val DayFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale.ENGLISH)

/** "Move LKR 39,900 to Savings now?" with the three steps from the Figma screen. */
@Composable
private fun MoveSavingsCard(amount: Long, fromAccount: String?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Move LKR ${formatAmount(amount)} to Savings now?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            val from = fromAccount?.let { " from $it" }.orEmpty()
            listOf(
                "Open your banking app",
                "Transfer ${formatAmount(amount)}$from to Savings",
                "Come back and tap the button below"
            ).forEachIndexed { index, step -> SavingsStep(number = index + 1, text = step) }
        }
    }
}

@Composable
private fun SavingsStep(number: Int, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** A short note in place of the move card: done for the month, or every goal reached. */
@Composable
private fun StatusCard(title: String, message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(MoneyTrailIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun SingleGoalCard(
    goal: GoalModel,
    progressColor: Color,
    trackColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = MoneyTrailIcons.Edit,
                        contentDescription = "Edit ${goal.name}",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = MoneyTrailIcons.Trash,
                        contentDescription = "Remove ${goal.name}",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Circular Gauge for this Goal
            GoalProgressGauge(
                progressFraction = goal.progressFraction,
                percentageText = goal.percentageText,
                progressColor = progressColor,
                trackColor = trackColor
            )

            // Goal Title & Saved Amount
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = goal.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "LKR ${formatAmount(goal.savedAmount)} of ${formatAmount(goal.targetAmount)} saved",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            if (goal.isReached) {
                Text(
                    text = "Goal reached. Well done!",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                return@Column
            }

            if (goal.isOverdue) {
                Text(
                    text = "The target date has passed. Edit the goal to pick a new one; until then the whole rest is due now.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            // Timeline Context & Recommended Monthly Savings Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Monthly Salary Target",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${goal.monthsRemaining} mo left",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "LKR ${formatAmount(goal.monthlySavingNeeded)} / month",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = goal.timelineSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Formula: ${goal.formulaExplanation}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalProgressGauge(
    progressFraction: Float,
    percentageText: String,
    modifier: Modifier = Modifier,
    trackColor: Color,
    progressColor: Color,
    strokeWidth: Dp = 18.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000),
        label = "GoalGaugeProgress"
    )

    Box(
        modifier = modifier.size(190.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = strokeWidth.toPx()
            val arcSize = size.minDimension - strokeWidthPx
            val topLeftOffset = Offset(strokeWidthPx / 2, strokeWidthPx / 2)
            val arcSizeDimensions = Size(arcSize, arcSize)

            // Background Ring Track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeftOffset,
                size = arcSizeDimensions,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )

            // Progress Fill Arc
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = (360f * animatedProgress).coerceAtLeast(8f),
                useCenter = false,
                topLeft = topLeftOffset,
                size = arcSizeDimensions,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = percentageText,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "of your goal",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GoalScreenPreview() {
    MoneyTrailTheme {
        GoalContent(
            state = GoalUiState.Success(
                goals = listOf(
                    GoalModel(
                        name = "MacBook Pro M4",
                        targetAmount = 490000L,
                        savedAmount = 11200L,
                        targetDate = LocalDate.of(2027, 10, 19)
                    ),
                    GoalModel(
                        name = "Emergency Fund",
                        targetAmount = 200000L,
                        savedAmount = 50000L,
                        targetDate = LocalDate.of(2027, 6, 1)
                    )
                ),
                salaryConfirmedText = "Salary confirmed: LKR 132,400 (bonus 12,400)",
                fromAccount = "Main bank",
                totalAllocationPool = 39_900L
            ),
            onMovedItClick = {},
            onRemindTomorrowClick = {},
            onOpenNewGoalDialog = {},
            onEditGoal = {},
            onDeleteGoal = {}
        )
    }
}
