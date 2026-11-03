package lk.moneytrail.app.presentation.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import lk.moneytrail.app.domain.model.Categories
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryStatus
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.IncomeDependability
import lk.moneytrail.app.domain.model.IncomeSource
import lk.moneytrail.app.domain.model.MonthBySource
import lk.moneytrail.app.domain.model.label
import lk.moneytrail.app.presentation.components.formatAmount
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme
import java.time.LocalDate

// MoneyTrail-Figma-HTML/11-IncomeInsights.html

/** The sources in the chart, bottom of each bar first. Crypto has its own card. */
private val ChartSources = listOf(IncomeSource.SALARY, IncomeSource.FREELANCE, IncomeSource.ADSENSE, IncomeSource.OTHER)

/** One colour per source, from the theme so they read in light and dark. */
@Composable
private fun sourceColor(source: IncomeSource): Color {
    val colors = MaterialTheme.colorScheme
    return when (source) {
        IncomeSource.SALARY -> colors.onSurface
        IncomeSource.FREELANCE -> colors.tertiary
        IncomeSource.ADSENSE -> colors.outline
        else -> colors.outlineVariant
    }
}

@Composable
internal fun IncomeInsights(uiState: InsightsState) {
    val colors = MaterialTheme.colorScheme
    val month = uiState.month.format(ShortMonthFormatter)
    val lastMonth = uiState.lastFullMonth.format(ShortMonthFormatter)
    val chart = uiState.incomeBySource

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // What arrived versus what is still only planned, so he stops guessing.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IncomeTotal(label = "Confirmed · $month", amount = uiState.income.confirmed, modifier = Modifier.weight(1f))
            IncomeTotal(
                label = "Expected",
                amount = uiState.income.expected,
                quiet = true,
                modifier = Modifier.weight(1f)
            )
        }

        InsightCard(
            title = "By source · ${InsightsState.CHART_MONTHS} months",
            note = chart.dropLast(1).lastOrNull()?.let { "${it.month.format(ShortMonthFormatter)}: LKR ${formatAmount(it.total)}" }
        ) {
            if (chart.all { it.total == 0L }) {
                QuietText("No confirmed income yet. Confirm your salary or add income on the calendar to see it here.")
            } else {
                SourceChart(chart)
                // Last month's amount for each source that brought something in.
                val previous = chart.dropLast(1).last()
                ChartSources.filter { previous.amount(it) > 0 }.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { source ->
                            SplitValue(
                                label = source.label,
                                amount = previous.amount(source),
                                color = sourceColor(source),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
        }

        InsightCard(title = "How dependable is it? · $lastMonth") {
            val split = uiState.dependability
            if (split.total == 0L) {
                QuietText("Nothing confirmed for $lastMonth yet.")
            } else {
                DependabilityBar(split)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SplitValue(
                        label = "Dependable (base salary) · ${split.dependablePercent}%",
                        amount = split.dependable,
                        color = colors.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    SplitValue(
                        label = "Variable · ${split.variablePercent}%",
                        amount = split.variable,
                        color = colors.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }
                QuietText("Plan savings on the dependable part; treat the rest as a bonus.")
            }
        }

        InsightCard(title = "Crypto · ${uiState.today.year}", note = "Speculative") {
            Text(
                text = "LKR ${formatAmount(uiState.cryptoThisYear)}",
                style = MaterialTheme.typography.titleMedium
            )
            QuietText("Crypto income you've recorded this year. It's left out of the chart and of savings planning, as it can swing either way.")
        }
    }
}

@Composable
private fun IncomeTotal(label: String, amount: Long, modifier: Modifier = Modifier, quiet: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = colors.outline
        )
        Text(
            text = formatAmount(amount),
            style = if (quiet) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.displaySmall,
            letterSpacing = (-0.02).em,
            color = if (quiet) colors.onSurfaceVariant else colors.onSurface
        )
    }
}

/** One stacked bar per month: salary at the bottom, then freelance, AdSense and other. */
@Composable
private fun SourceChart(months: List<MonthBySource>) {
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
                    if (month.total > 0) {
                        Column(
                            Modifier
                                .widthIn(max = 30.dp)
                                .fillMaxWidth()
                                .fillMaxHeight(month.total.toFloat() / biggest),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            // Top of the bar first, so salary ends up at the bottom.
                            ChartSources.reversed().filter { month.amount(it) > 0 }.forEachIndexed { index, source ->
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .weight(month.amount(source).toFloat())
                                        .background(
                                            sourceColor(source).copy(alpha = if (isCurrent) 1f else 0.75f),
                                            if (index == 0) RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp) else RoundedCornerShape(0.dp)
                                        )
                                )
                            }
                        }
                    }
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
                    fontWeight = FontWeight.SemiBold,
                    color = colors.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Dependable (dark) and variable (orange) side by side, each as wide as its share. */
@Composable
private fun DependabilityBar(split: IncomeDependability) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(4.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clearAndSetSemantics {
                contentDescription = "Dependable ${formatAmount(split.dependable)} (${split.dependablePercent}%), " +
                    "variable ${formatAmount(split.variable)} (${split.variablePercent}%)"
            },
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (split.dependable > 0) {
            Box(
                Modifier
                    .weight(split.dependable.toFloat())
                    .fillMaxHeight()
                    .background(colors.onSurface, shape)
            )
        }
        if (split.variable > 0) {
            Box(
                Modifier
                    .weight(split.variable.toFloat())
                    .fillMaxHeight()
                    .background(colors.tertiary, shape)
            )
        }
    }
}

private fun previewIncome(id: String, amount: Long, category: String, date: LocalDate, bonus: Long = 0, expected: Boolean = false) =
    Entry(
        id, EntryType.INCOME, amount, category, "Main bank", date, "", null,
        status = if (expected) EntryStatus.EXPECTED else EntryStatus.CONFIRMED,
        bonus = bonus
    )

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun IncomeInsightsPreview() {
    val today = LocalDate.of(2026, 10, 10)
    MoneyTrailTheme {
        IncomeInsights(
            InsightsState(
                today = today,
                isLoading = false,
                tab = InsightsTab.INCOME,
                entries = listOf(
                    previewIncome("1", 133_400, Categories.SALARY, LocalDate.of(2026, 9, 25), bonus = 13_400),
                    previewIncome("2", 35_000, "Freelance", LocalDate.of(2026, 9, 12)),
                    previewIncome("3", 12_600, Categories.ADSENSE, LocalDate.of(2026, 9, 5)),
                    previewIncome("4", 126_000, Categories.SALARY, LocalDate.of(2026, 8, 25), bonus = 6_000),
                    previewIncome("5", 56_210, "Freelance", today),
                    previewIncome("6", 130_000, Categories.SALARY, LocalDate.of(2026, 10, 25), expected = true),
                    previewIncome("7", 4_200, "Crypto", LocalDate.of(2026, 3, 2))
                )
            )
        )
    }
}
