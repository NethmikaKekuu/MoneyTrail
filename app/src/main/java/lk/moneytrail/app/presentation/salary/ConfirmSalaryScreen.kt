package lk.moneytrail.app.presentation.salary

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.moneytrail.app.presentation.components.EmptyView
import lk.moneytrail.app.presentation.components.LoadingView
import lk.moneytrail.app.presentation.components.MoneyTrailIcons
import lk.moneytrail.app.presentation.components.formatAmount
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme

// MoneyTrail-Figma-HTML/13-SalaryConfirm.html, kept to one task at a time: this month's
// salary on top, everything else one line each under it.
@Composable
fun ConfirmSalaryScreen(
    onBackClick: () -> Unit,
    onConfirmedNavigate: () -> Unit,
    onSetUpClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConfirmSalaryViewModel = hiltViewModel()
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
        topBar = { ConfirmSalaryTopBar(onBackClick = onBackClick) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> LoadingView(message = "Loading salary info…")
                !uiState.hasPlan -> EmptyView(
                    title = "No salary set up",
                    message = "You skipped setup, so there's no salary or savings goal yet.",
                    actionLabel = "Set up salary and goal",
                    onAction = onSetUpClick
                )
                else -> ConfirmSalaryContent(
                    state = uiState,
                    onAmountChange = viewModel::onAmountCreditedChange,
                    onConfirmSalary = { viewModel.onConfirmSalary(onConfirmed = onConfirmedNavigate) },
                    onNotYetClick = onBackClick,
                    onEditSalaryClick = viewModel::onEditSalaryClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (uiState.isEditingSalary) {
        EditSalaryDialog(
            state = uiState,
            onPayDayChange = viewModel::onPayDayChange,
            onBaseSalaryChange = viewModel::onBaseSalaryChange,
            onSave = viewModel::onSaveSalaryClick,
            onDismiss = viewModel::onEditSalaryDismiss
        )
    }
}

@Composable
private fun ConfirmSalaryTopBar(onBackClick: () -> Unit) {
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
            text = "Confirm salary",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .padding(end = 44.dp)
        )
    }
}

@Composable
private fun ConfirmSalaryContent(
    state: ConfirmSalaryUiState,
    onAmountChange: (String) -> Unit,
    onConfirmSalary: () -> Unit,
    onNotYetClick: () -> Unit,
    onEditSalaryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (state.hasSalary) {
            SalaryCard(state = state, onAmountChange = onAmountChange)

            Button(
                onClick = onConfirmSalary,
                enabled = !state.isSubmitting && state.totalCredited > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (state.isConfirmed) "Update salary" else "Confirm salary",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!state.isConfirmed) {
                TextButton(onClick = onNotYetClick, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Not yet, remind me later", fontWeight = FontWeight.Bold)
                }
            }

            if (state.hasBonusHistory) {
                Text(
                    text = "Bonus over the last months: average LKR ${formatAmount(state.averageBonus)}, " +
                        "highest LKR ${formatAmount(state.highestBonus)} in ${state.highestBonusMonth}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            Text(
                text = "No salary to confirm yet. Set your pay day and base salary below, and it will be ready to confirm on pay day.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SalarySettingsRow(state = state, onEditClick = onEditSalaryClick)
    }
}

/** This month's salary: what arrived, already filled in with the usual amount. */
@Composable
private fun SalaryCard(state: ConfirmSalaryUiState, onAmountChange: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state.monthTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (state.isConfirmed) colors.primaryContainer else colors.tertiary
                ) {
                    Text(
                        text = state.statusText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (state.isConfirmed) colors.onPrimaryContainer else colors.onTertiary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "How much arrived in ${state.accountName}? Check your banking app.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, colors.tertiary, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("LKR", style = MaterialTheme.typography.titleMedium, color = colors.outline)
                BasicTextField(
                    value = state.amountCreditedDigits.toLongOrNull()?.let { formatAmount(it) } ?: state.amountCreditedDigits,
                    onValueChange = onAmountChange,
                    textStyle = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors.onSurface),
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = breakdown(state),
                style = MaterialTheme.typography.bodySmall,
                color = if (state.bonusAmount < 0 && state.baseSalary > 0) colors.error else colors.outline
            )
        }
    }
}

/** How the amount splits into the base pay and this month's bonus. */
private fun breakdown(state: ConfirmSalaryUiState): String {
    val base = state.baseSalary
    val bonus = state.bonusAmount
    return when {
        state.totalCredited == 0L -> "Type the amount your bank shows."
        base == 0L -> "All of it counts as this month's salary."
        bonus > 0 -> "Base ${formatAmount(base)} + bonus ${formatAmount(bonus)}"
        bonus == 0L -> "Your usual base salary, no bonus this month."
        else -> "${formatAmount(-bonus)} less than your base. It's saved as it is."
    }
}

/** "Pay day and base salary · 25th of the month · LKR 120,000" with Change. */
@Composable
private fun SalarySettingsRow(state: ConfirmSalaryUiState, onEditClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onEditClick,
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Pay day and base salary", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (state.baseSalary > 0 && state.payDay != null) {
                        "${ordinal(state.payDay)} of the month · LKR ${formatAmount(state.baseSalary)}"
                    } else {
                        "Not set up yet"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.outline
                )
            }
            Text(
                text = if (state.baseSalary > 0) "Change" else "Set up",
                style = MaterialTheme.typography.labelLarge,
                color = colors.primary
            )
        }
    }
}

/** 1st, 2nd, 3rd, 4th … 21st, 22nd, 23rd, 31st. */
private fun ordinal(day: Int): String {
    val suffix = if (day in 11..13) "th" else when (day % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
    return "$day$suffix"
}

// Sample figures for the previews only, from the assignment scenario.
private val PreviewState = ConfirmSalaryUiState(
    monthTitle = "October salary",
    statusText = "Due 25 Oct",
    accountName = "Main bank",
    baseSalary = 120_000L,
    bonusAmount = 12_400L,
    amountCreditedDigits = "132400",
    payDay = 25,
    bonusHistory = listOf(BonusHistoryItem("Aug", 18_200L), BonusHistoryItem("Oct", 12_400L, isCurrentMonth = true)),
    averageBonus = 15_300L,
    highestBonus = 18_200L,
    highestBonusMonth = "Aug",
    isLoading = false
)

@Preview(showBackground = true)
@Composable
private fun ConfirmSalaryScreenPreview() {
    MoneyTrailTheme {
        ConfirmSalaryContent(
            state = PreviewState,
            onAmountChange = {},
            onConfirmSalary = {},
            onNotYetClick = {},
            onEditSalaryClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfirmSalaryScreenNoSalaryPreview() {
    MoneyTrailTheme {
        ConfirmSalaryContent(
            state = ConfirmSalaryUiState(hasSalary = false, isLoading = false),
            onAmountChange = {},
            onConfirmSalary = {},
            onNotYetClick = {},
            onEditSalaryClick = {}
        )
    }
}
