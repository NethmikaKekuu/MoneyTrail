package lk.moneytrail.app.presentation.salary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.moneytrail.app.presentation.components.LoadingView
import lk.moneytrail.app.presentation.components.MoneyTrailIcons
import lk.moneytrail.app.presentation.components.formatAmount
import lk.moneytrail.app.presentation.components.formatNet
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme

@Composable
fun ConfirmSalaryScreen(
    onBackClick: () -> Unit,
    onConfirmedNavigate: () -> Unit,
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
        topBar = {
            ConfirmSalaryTopBar(onBackClick = onBackClick)
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
                uiState.isLoading -> LoadingView(message = "Loading salary info…")
                else -> {
                    ConfirmSalaryContent(
                        state = uiState,
                        onAmountChange = viewModel::onAmountCreditedChange,
                        onConfirmSalary = {
                            viewModel.onConfirmSalary(onConfirmed = onConfirmedNavigate)
                        },
                        onBackClick = onBackClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
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
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Payday Status Banner (Top Orange Card)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFFA5E00),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.paydayText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Ready to confirm",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFA5E00),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "Salary into ${state.accountName}. Check your banking app and enter exactly what arrived.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.95f),
                    lineHeight = 20.sp
                )
            }
        }

        // Base & Bonus Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Left Card: Base
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, Color(0xFFEBEBEB), RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Base (from profile)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = formatAmount(state.baseSalary),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Right Card: Bonus
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, Color(0xFFEBEBEB), RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Bonus this month",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = formatNet(state.bonusAmount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (state.bonusAmount >= 0) Color(0xFFC83803) else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Amount Actually Credited Field
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Amount actually credited (LKR)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFFF8F5))
                    .border(2.dp, Color(0xFFFA5E00), RoundedCornerShape(20.dp))
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                val formattedDisplay = state.amountCreditedDigits.toLongOrNull()?.let { formatAmount(it) }
                    ?: state.amountCreditedDigits

                BasicTextField(
                    value = formattedDisplay,
                    onValueChange = onAmountChange,
                    textStyle = TextStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(
                text = "Outside 120k–140k is fine; your pay changes and that's okay.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                fontSize = 13.sp
            )
        }

        // Bonus History Section
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFF2E4DC), RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Bonus history",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!state.hasBonusHistory) {
                    Text(
                        text = "No past bonus history available yet. Your history will appear here after your first few months.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                } else {
                    // Bar Chart
                    val maxAmount = (state.bonusHistory.maxOfOrNull { it.amount } ?: 1L).coerceAtLeast(1L)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        state.bonusHistory.forEach { item ->
                            val barHeightFraction = if (maxAmount > 0) {
                                (item.amount.toFloat() / maxAmount.toFloat()).coerceIn(0.2f, 1f)
                            } else 0.2f

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .fillMaxHeight(barHeightFraction)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (item.isCurrentMonth) Color(0xFFC83803) else Color(0xFFFCBD85)
                                        )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = item.monthLabel,
                                    fontSize = 12.sp,
                                    fontWeight = if (item.isCurrentMonth) FontWeight.Bold else FontWeight.Medium,
                                    color = if (item.isCurrentMonth) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    if (state.averageBonus > 0 || state.highestBonus > 0) {
                        Text(
                            text = "Average bonus: ${formatAmount(state.averageBonus)} · Highest: ${formatAmount(state.highestBonus)} (${state.highestBonusMonth})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action Buttons at Bottom
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onConfirmSalary,
                enabled = !state.isSubmitting && state.totalCredited > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFC83803)
                )
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Confirm salary",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            TextButton(onClick = onBackClick) {
                Text(
                    text = "Not yet, remind me later",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC83803)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfirmSalaryScreenPreview() {
    MoneyTrailTheme {
        ConfirmSalaryContent(
            state = ConfirmSalaryUiState(
                bonusHistory = listOf(
                    BonusHistoryItem("May", 10_000L),
                    BonusHistoryItem("Jun", 14_000L),
                    BonusHistoryItem("Jul", 8_000L),
                    BonusHistoryItem("Aug", 18_200L),
                    BonusHistoryItem("Sep", 15_000L),
                    BonusHistoryItem("Oct", 12_400L, isCurrentMonth = true)
                ),
                bonusAmount = 12_400L,
                amountCreditedDigits = "132400",
                averageBonus = 11_400L,
                highestBonus = 18_200L,
                highestBonusMonth = "Aug"
            ),
            onAmountChange = {},
            onConfirmSalary = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfirmSalaryScreenSingleMonthPreview() {
    MoneyTrailTheme {
        ConfirmSalaryContent(
            state = ConfirmSalaryUiState(
                bonusHistory = listOf(
                    BonusHistoryItem("May", 12_400L, isCurrentMonth = true)
                ),
                bonusAmount = 12_400L,
                amountCreditedDigits = "132400",
                averageBonus = 12_400L,
                highestBonus = 12_400L,
                highestBonusMonth = "May"
            ),
            onAmountChange = {},
            onConfirmSalary = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfirmSalaryScreenEmptyHistoryPreview() {
    MoneyTrailTheme {
        ConfirmSalaryContent(
            state = ConfirmSalaryUiState(
                bonusHistory = emptyList(),
                bonusAmount = 0L,
                amountCreditedDigits = "120000"
            ),
            onAmountChange = {},
            onConfirmSalary = {},
            onBackClick = {}
        )
    }
}
