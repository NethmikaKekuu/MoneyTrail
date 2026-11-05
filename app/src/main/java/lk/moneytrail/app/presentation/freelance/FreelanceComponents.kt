package lk.moneytrail.app.presentation.freelance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import lk.moneytrail.app.domain.model.Project
import lk.moneytrail.app.presentation.components.MoneyTrailIcons

/** Back button, centred title and an optional action icon, all 44 dp so the title sits in the middle. */
@Composable
internal fun FreelanceTopBar(
    title: String,
    onBackClick: () -> Unit,
    actionIcon: ImageVector? = null,
    actionDescription: String? = null,
    onActionClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TopBarIcon(MoneyTrailIcons.ChevronLeft, "Back", onBackClick, MaterialTheme.colorScheme.onSurface)
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )
        if (actionIcon != null) {
            TopBarIcon(actionIcon, actionDescription, onActionClick, MaterialTheme.colorScheme.primary)
        } else {
            Spacer(Modifier.size(44.dp))
        }
    }
}

@Composable
private fun TopBarIcon(icon: ImageVector, description: String?, onClick: () -> Unit, tint: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = description, tint = tint)
    }
}

/** Paid, then invoiced, then the rest of the price. */
@Composable
internal fun ProjectProgressBar(project: Project, modifier: Modifier = Modifier) {
    val budget = maxOf(project.totalBudget, project.totalMilestonesAmount, 1L).toFloat()
    val paid = (project.paidAmount / budget).coerceIn(0f, 1f)
    val invoiced = (project.invoicedAmount / budget).coerceIn(0f, 1f - paid)
    val rest = (1f - paid - invoiced).coerceAtLeast(0f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (paid > 0f) Box(Modifier.weight(paid).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
        if (invoiced > 0f) Box(Modifier.weight(invoiced).fillMaxHeight().background(MaterialTheme.colorScheme.tertiary))
        if (rest > 0f) Spacer(Modifier.weight(rest))
    }
}

/** Small coloured legend dot and label, e.g. "Paid 30,000". */
@Composable
internal fun LegendItem(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Rounded status label; [emphasis] picks the colours. */
@Composable
internal fun StatusChip(label: String, emphasis: ChipEmphasis) {
    val colors = MaterialTheme.colorScheme
    val (bg, fg) = when (emphasis) {
        ChipEmphasis.ALERT -> colors.errorContainer to colors.onErrorContainer
        ChipEmphasis.ACTIVE -> colors.tertiaryContainer to colors.onTertiaryContainer
        ChipEmphasis.DONE -> colors.primary to colors.onPrimary
        ChipEmphasis.QUIET -> colors.surfaceVariant to colors.onSurfaceVariant
    }
    Surface(color = bg, shape = RoundedCornerShape(12.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

enum class ChipEmphasis { ALERT, ACTIVE, DONE, QUIET }
