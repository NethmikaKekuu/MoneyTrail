package lk.moneytrail.app.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lk.moneytrail.app.presentation.components.MoneyTrailIcons

// Pieces of the onboarding screen, styled after MoneyTrail-Figma-HTML/06-Onboarding.html.

/** The four-part bar under the header; parts up to [current] are filled. */
@Composable
internal fun StepProgress(current: Int, total: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Step $current of $total" },
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(total) { index ->
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(
                        color = if (index < current) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}

/** A tickable card with a letter tile, a name and a line about it, e.g. "Main bank". */
@Composable
internal fun ChoiceCard(
    letter: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(shape)
            .background(if (selected) colors.surfaceContainerLow else colors.surface)
            .border(1.5.dp, if (selected) colors.tertiary else colors.outlineVariant, shape)
            .toggleable(value = selected, enabled = enabled, role = Role.Checkbox, onValueChange = { onClick() })
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = if (selected) colors.primaryContainer else colors.outlineVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = letter,
                style = MaterialTheme.typography.titleSmall,
                fontSize = 16.sp,
                color = if (selected) colors.onPrimaryContainer else colors.outline
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Normal, color = colors.outline)
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .then(
                    if (selected) Modifier.background(colors.primary)
                    else Modifier.border(2.dp, colors.outlineVariant, RoundedCornerShape(8.dp))
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(MoneyTrailIcons.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** Days 1 to 31 in rows of seven; the picked day is filled. */
@Composable
internal fun PayDayGrid(
    selectedDay: Int?,
    onDayClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        (1..31).chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEach { day ->
                    val isSelected = day == selectedDay
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) colors.primary else colors.surfaceContainerLow)
                            .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onDayClick(day) })
                            .semantics { contentDescription = "Day $day" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) colors.onPrimary else colors.onSurface
                        )
                    }
                }
                // Keep the last, shorter row's days the same width as the others.
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
