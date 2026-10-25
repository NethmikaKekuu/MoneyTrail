package lk.moneytrail.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme

/**
 * A strip shown while the phone is offline. The screens still work from the copy on
 * the phone, so this only says so, and that new entries will sync later.
 */
@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.secondaryContainer)
            .padding(horizontal = 20.dp, vertical = 8.dp)
            // Read out by TalkBack when it appears.
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = MoneyTrailIcons.CloudOff,
            contentDescription = null,
            tint = colors.onSecondaryContainer,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = "You're offline. Showing what's saved on this phone; new entries sync when you're back online.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSecondaryContainer
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OfflineBannerPreview() {
    MoneyTrailTheme {
        OfflineBanner()
    }
}
