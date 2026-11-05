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

