package lk.moneytrail.app.presentation.goal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.moneytrail.app.domain.model.MoneyPlan
import lk.moneytrail.app.domain.model.SavingsGoal
import lk.moneytrail.app.presentation.components.EmptyView
import lk.moneytrail.app.presentation.components.ErrorView
import lk.moneytrail.app.presentation.components.LoadingView
import lk.moneytrail.app.presentation.components.MoneyTrailIcons
import lk.moneytrail.app.presentation.components.formatAmount
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Figma 14: Goal Progress
@Composable
fun GoalProgressScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GoalProgressViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GoalProgressContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onRetryClick = viewModel::onRetryClick,
        modifier = modifier
    )
}

private val TargetDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

@Composable
private fun GoalProgressContent(
    uiState: GoalProgressState,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedIconButton(
                onClick = onBackClick,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, colors.outlineVariant),
                colors = IconButtonDefaults.outlinedIconButtonColors(containerColor = colors.surface),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(MoneyTrailIcons.ChevronLeft, contentDescription = "Back", modifier = Modifier.size(20.dp))
            }
            Text(
                text = "Goal Progress",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(44.dp))
        }

        when {
            uiState.errorMessage != null -> ErrorView(
                title = "Couldn't load goal progress",
                message = uiState.errorMessage,
                onRetry = onRetryClick
            )
            uiState.isLoading -> LoadingView(message = "Loading goal progress…")
            uiState.plan == null -> EmptyView(
                title = "No savings goal set",
                message = "Set up your savings goal in Onboarding or Settings to start tracking progress."
            )
            else -> GoalProgressDetails(uiState, onBackClick)
        }
    }
}

@Composable
private fun GoalProgressDetails(
    uiState: GoalProgressState,
    onBackClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val formattedPercentage = String.format(Locale.US, "%.1f", uiState.percentage)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Goal Hero Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "SAVINGS GOAL",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                        Text(
                            text = uiState.goalName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (uiState.isGoalReached) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.secondaryContainer
                        ) {
                            Text(
                                text = "Goal Reached 🎉",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "LKR ${formatAmount(uiState.savedAmount)}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp,
                        color = colors.onSurface
                    )
                    Text(
                        text = "saved of LKR ${formatAmount(uiState.targetAmount)} target",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.outline
                    )
                }

                // Progress Bar & Percentage
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Progress",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurfaceVariant
                        )
                        Text(
                            text = "$formattedPercentage%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                    }

                    LinearProgressIndicator(
                        progress = { uiState.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = colors.tertiary,
                        trackColor = colors.surfaceContainerLow
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceContainerLow, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.outline
                    )
                    Text(
                        text = "LKR ${formatAmount(uiState.remainingAmount)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                }
            }
        }

        // Details Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Goal Target Details",
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Target Date", style = MaterialTheme.typography.bodyMedium, color = colors.outline)
                    Text(
                        text = uiState.targetDate?.format(TargetDateFormatter) ?: "Not set",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Monthly Saving Needed", style = MaterialTheme.typography.bodyMedium, color = colors.outline)
                    Text(
                        text = if (uiState.isGoalReached) "Goal reached" else "LKR ${formatAmount(uiState.monthlySavingNeeded)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.isGoalReached) colors.primary else colors.onSurface
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onBackClick,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Done", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GoalProgressScreenPreview() {
    MoneyTrailTheme {
        GoalProgressContent(
            uiState = GoalProgressState(
                isLoading = false,
                plan = MoneyPlan(
                    payDay = 25,
                    baseSalary = 120_000,
                    incomeSources = emptySet(),
                    goal = SavingsGoal(
                        name = "MacBook",
                        targetAmount = 490_000,
                        savedAmount = 11_200,
                        targetDate = LocalDate.of(2027, 10, 19)
                    )
                ),
                transferSavingsTotal = 39_900
            ),
            onBackClick = {},
            onRetryClick = {}
        )
    }
}
