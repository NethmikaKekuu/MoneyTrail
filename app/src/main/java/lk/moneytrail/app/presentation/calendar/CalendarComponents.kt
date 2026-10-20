package lk.moneytrail.app.presentation.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryTotals
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.SpendingType
import lk.moneytrail.app.presentation.components.MINUS_SIGN
import lk.moneytrail.app.presentation.components.MoneyTrailIcons
import lk.moneytrail.app.presentation.components.formatAmount
import lk.moneytrail.app.presentation.components.formatNet
import lk.moneytrail.app.presentation.theme.Ink
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// Pieces of the calendar screen, styled after MoneyTrail-Figma-HTML/07-Main.html.

private val WeekdayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

/** 44dp outlined square button, used for the month arrows. */
@Composable
internal fun SquareIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedIconButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.size(44.dp)
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(18.dp))
    }
}

/** The orange card with the month's confirmed income and expenses, and what's still expected. */
@Composable
internal fun MonthSummary(totals: EntryTotals, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryItem("Income", formatAmount(totals.income), Modifier.weight(1f))
        SummaryItem("Expenses", formatAmount(totals.expenses), Modifier.weight(1f))
        // Expected money in minus expected money out, e.g. "+30,000" for an unpaid invoice.
        SummaryItem("Expected", formatNet(totals.expectedIncome - totals.expectedExpenses), Modifier.weight(1f))
    }
}

@Composable
private fun SummaryItem(label: String, value: String, modifier: Modifier = Modifier) {
    // The card is always orange, so its text stays dark in both themes.
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = Ink)
        Text(value, style = MaterialTheme.typography.titleMedium, fontSize = 16.sp, color = Ink, maxLines = 1)
    }
}

@Composable
internal fun MonthGrid(
    days: List<CalendarDay>,
    selectedDate: LocalDate,
    today: LocalDate,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.padding(bottom = 2.dp)) {
            WeekdayLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        days.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                week.forEach { day ->
                    DayCell(
                        day = day,
                        isSelected = day.date == selectedDate,
                        isToday = day.date == today,
                        onClick = { onDayClick(day.date) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)
        ) {
            LegendItem("Income") { Marker(MaterialTheme.colorScheme.secondary, CircleShape, size = 7) }
            LegendItem("Expense") { Marker(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(1.dp), size = 7) }
            LegendItem("Expected") { ExpectedMarker(MaterialTheme.colorScheme.secondary, size = 7) }
        }
    }
}

@Composable
private fun DayCell(
    day: CalendarDay,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val textColor = when {
        !day.isInMonth -> colors.outline.copy(alpha = 0.45f)
        isSelected -> colors.onPrimary
        isToday -> colors.primary
        else -> colors.onSurface
    }
    val semantics = if (day.isInMonth) {
        val month = day.date.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        val entries = if (day.entryCount == 1) "1 entry" else "${day.entryCount} entries"
        Modifier.semantics { contentDescription = "${day.date.dayOfMonth} $month, $entries" }
    } else {
        // Days of the next or previous month only fill the grid.
        Modifier.clearAndSetSemantics {}
    }

    Column(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) colors.primary else Color.Transparent)
            .clickable(enabled = day.isInMonth, role = Role.Button, onClick = onClick)
            .then(semantics),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 14.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
            color = textColor
        )
        Spacer(Modifier.height(3.dp))
        Row(
            modifier = Modifier.height(6.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val markerOnSelected = colors.onPrimary.copy(alpha = 0.75f)
            if (day.hasIncome) {
                Marker(if (isSelected) markerOnSelected else colors.secondary, CircleShape)
            }
            if (day.hasExpense) {
                Marker(if (isSelected) markerOnSelected else colors.tertiary, RoundedCornerShape(1.dp))
            }
            if (day.hasExpected) {
                ExpectedMarker(if (isSelected) markerOnSelected else colors.secondary)
            }
        }
    }
}

/** Income days get a round marker and expense days a square one, as in the design. */
@Composable
private fun Marker(color: Color, shape: Shape, size: Int = 6) {
    Box(
        Modifier
            .size(size.dp)
            .background(color, shape)
    )
}

/** A dashed ring for days with money expected but not yet confirmed, as in the design. */
@Composable
private fun ExpectedMarker(color: Color, size: Int = 6) {
    Canvas(Modifier.size(size.dp)) {
        val stroke = 1.dp.toPx()
        drawCircle(
            color = color,
            radius = (this.size.minDimension - stroke) / 2,
            style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 1.5f)))
        )
    }
}

@Composable
private fun LegendItem(label: String, marker: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        marker()
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

/** One entry in the selected day's list, with its note underneath if it has one. Tap to edit it. */
@Composable
internal fun EntryCard(entry: Entry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    val isIncome = entry.type == EntryType.INCOME
    val kind = if (entry.isExpected) "Expected" else when (entry.spendingType) {
        SpendingType.COMMITTED -> "Committed"
        SpendingType.DISCRETIONARY -> "Discretionary"
        null -> "Income"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.outlineVariant, shape)
            .clickable(role = Role.Button, onClickLabel = "Edit", onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(colors.primaryContainer, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.category.take(1).uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 15.sp,
                    color = colors.onPrimaryContainer
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(entry.category, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "${entry.account} · $kind",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Normal,
                    color = colors.outline
                )
            }
            Text(
                text = (if (isIncome) "+ " else "$MINUS_SIGN ") + formatAmount(entry.amount),
                style = MaterialTheme.typography.titleSmall,
                fontSize = 15.sp,
                color = when {
                    entry.isExpected -> colors.outline
                    isIncome -> colors.secondary
                    else -> colors.onSurface
                },
                maxLines = 1
            )
        }
        if (entry.note.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceContainerLow, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = MoneyTrailIcons.Note,
                    contentDescription = "Note:",
                    tint = colors.primary,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(14.dp)
                )
                Text(
                    text = entry.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}
