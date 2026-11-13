package lk.moneytrail.app.presentation.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import lk.moneytrail.app.presentation.components.MoneyTrailIcons
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme

/** "2 reminders waiting" with a View button, at the top of the calendar while any are unread. */
@Composable
internal fun RemindersBanner(
    count: Int,
    onOpenClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onOpenClick,
        shape = RoundedCornerShape(16.dp),
        color = colors.primaryContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(MoneyTrailIcons.Bell, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (count == 1) "1 reminder waiting" else "$count reminders waiting",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onPrimaryContainer
                )
                Text(
                    text = "Money due, savings or invoices that need you.",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Normal,
                    color = colors.onPrimaryContainer
                )
            }
            Text("View", style = MaterialTheme.typography.labelLarge, color = colors.onPrimaryContainer)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RemindersBannerPreview() {
    MoneyTrailTheme {
        RemindersBanner(count = 2, onOpenClick = {})
    }
}
