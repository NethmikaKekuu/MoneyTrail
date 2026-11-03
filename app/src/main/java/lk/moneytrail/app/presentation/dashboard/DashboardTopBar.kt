package lk.moneytrail.app.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import lk.moneytrail.app.presentation.calendar.SquareIconButton
import lk.moneytrail.app.presentation.components.MoneyTrailIcons
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme

/**
 * The row above every tab: the bell on the far left, which opens the notification list with the number waiting,
 * and the menu on the far right, which opens and closes the More page.
 */
@Composable
internal fun DashboardTopBar(
    isMenuOpen: Boolean,
    unreadNotifications: Int,
    onMenuClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BadgedBox(
            badge = {
                if (unreadNotifications > 0) Badge { Text(if (unreadNotifications > 9) "9+" else unreadNotifications.toString()) }
            }
        ) {
            SquareIconButton(
                icon = MoneyTrailIcons.Bell,
                contentDescription = if (unreadNotifications > 0) "Notifications, $unreadNotifications waiting" else "Notifications",
                onClick = onNotificationsClick
            )
        }
        SquareIconButton(
            icon = MoneyTrailIcons.Menu,
            contentDescription = if (isMenuOpen) "Close menu" else "Open menu",
            onClick = onMenuClick
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardTopBarPreview() {
    MoneyTrailTheme {
        DashboardTopBar(isMenuOpen = false, unreadNotifications = 3, onMenuClick = {}, onNotificationsClick = {})
    }
}
