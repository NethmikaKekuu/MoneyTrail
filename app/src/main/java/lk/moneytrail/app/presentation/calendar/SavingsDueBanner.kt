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
import androidx.compose.ui.unit.sp
import lk.moneytrail.app.presentation.components.MoneyTrailIcons
import lk.moneytrail.app.presentation.components.formatAmount
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme

/** "Pay yourself first · LKR 39,900 to move" with an Open button, while this month's saving is due. */
@Composable
internal fun SavingsDueBanner(
    amount: Long,
    onOpenClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onOpenClick,
        shape = RoundedCornerShape(16.dp),
        color = colors.secondaryContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(MoneyTrailIcons.Goal, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Pay yourself first · LKR ${formatAmount(amount)} to move",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSecondaryContainer
                )
                Text(
                    text = "Move it to Savings now, before it gets spent.",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Normal,
                    color = colors.onSecondaryContainer
                )
            }
            Text("Open", style = MaterialTheme.typography.labelLarge, fontSize = 14.sp, color = colors.primary)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SavingsDueBannerPreview() {
    MoneyTrailTheme {
        SavingsDueBanner(amount = 39_900, onOpenClick = {})
    }
}
