package lk.moneytrail.app.presentation.notifications

import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.moneytrail.app.domain.model.AppNotification
import lk.moneytrail.app.domain.model.NotificationKind
import lk.moneytrail.app.presentation.auth.AuthBackButton
import lk.moneytrail.app.presentation.components.EmptyView
import lk.moneytrail.app.presentation.components.LoadingView
import lk.moneytrail.app.presentation.components.MoneyTrailIcons
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DayFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

/**
 * The reminders still waiting for the user, opened from the bell. Tapping one shows what
 * it asks for with a button to do it now and "Later", which leaves it here unread. A
 * reminder disappears once what it asks for has been done.
 */
@Composable
fun NotificationsScreen(
    onBackClick: () -> Unit,
    onAction: (NotificationKind) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Coming back from doing the action: whatever is now done leaves the list.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }

    NotificationsContent(
        uiState = uiState,
        today = LocalDate.now(),
        // Like other apps: tapping a reminder opens the place it's about.
        onItemClick = { item ->
            viewModel.onItemClick(item)
            onAction(item.kind)
        },
        onBackClick = onBackClick,
        modifier = modifier
    )
}

@Composable
private fun NotificationsContent(
    uiState: NotificationsState,
    today: LocalDate,
    onItemClick: (AppNotification) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            AuthBackButton(onClick = onBackClick, contentDescription = "Back", modifier = Modifier.align(Alignment.CenterStart))
            Text("Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }

        when {
            uiState.isLoading -> LoadingView()
            uiState.items.isEmpty() -> EmptyView(
                title = "No notifications yet",
                message = "Reminders about money due, savings, invoices and catching up show here, and stay for a month."
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (uiState.unread.isNotEmpty()) {
                    item(key = "new") { SectionLabel("New") }
                    items(uiState.unread, key = { it.id }) { item ->
                        NotificationCard(item, today, onClick = { onItemClick(item) })
                    }
                }
                if (uiState.earlier.isNotEmpty()) {
                    item(key = "earlier") { SectionLabel("Earlier", Modifier.padding(top = 8.dp)) }
                    items(uiState.earlier, key = { it.id }) { item ->
                        NotificationCard(item, today, onClick = { onItemClick(item) })
                    }
                }
            }
        }
    }
}

/** A small uppercase heading, as on the More and Settings pages. */
@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(Locale.ENGLISH),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.7.sp,
        color = MaterialTheme.colorScheme.outline,
        modifier = modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}

/** One reminder as a card: unread ones in the warm accent with a dot, read ones quieter. */
@Composable
private fun NotificationCard(item: AppNotification, today: LocalDate, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (item.isRead) colors.surfaceContainerLow else colors.primaryContainer.copy(alpha = 0.55f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KindIcon(item.kind)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (item.isRead) FontWeight.Medium else FontWeight.Bold
                )
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 2
                )
                Text(
                    text = "${dayText(item.day, today)} · ${item.kind.actionLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (item.isRead) colors.outline else colors.primary
                )
            }
            if (!item.isRead) {
                Box(Modifier.size(10.dp).background(colors.tertiary, CircleShape))
            }
        }
    }
}

@Composable
private fun KindIcon(kind: NotificationKind, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(40.dp)
            .background(colors.primaryContainer, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(kind.icon, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(20.dp))
    }
}


private val NotificationKind.icon: ImageVector
    get() = when (this) {
        NotificationKind.MONEY_DUE -> MoneyTrailIcons.Calendar
        NotificationKind.PAY_YOURSELF_FIRST -> MoneyTrailIcons.Goal
        NotificationKind.CATCH_UP -> MoneyTrailIcons.Check
        NotificationKind.INVOICE_OVERDUE -> MoneyTrailIcons.Card
    }

private val NotificationKind.actionLabel: String
    get() = when (this) {
        NotificationKind.MONEY_DUE -> "Check now"
        NotificationKind.PAY_YOURSELF_FIRST -> "Open savings"
        NotificationKind.CATCH_UP -> "Catch up now"
        NotificationKind.INVOICE_OVERDUE -> "Open projects"
    }

private fun dayText(day: LocalDate, today: LocalDate): String = when (day) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> day.format(DayFormatter)
}

@Preview(showBackground = true)
@Composable
private fun NotificationsScreenPreview() {
    val today = LocalDate.of(2026, 10, 30)
    MoneyTrailTheme {
        NotificationsContent(
            uiState = NotificationsState(
                isLoading = false,
                items = listOf(
                    AppNotification(1, NotificationKind.PAY_YOURSELF_FIRST, "Pay yourself first", "Move LKR 39,900 to Savings now, before it gets spent.", today, 2),
                    AppNotification(2, NotificationKind.MONEY_DUE, "Freelance expected today", "LKR 85,000. Did it come in? Tap to check.", today.minusDays(1), 1, isRead = true)
                )
            ),
            today = today,
            onItemClick = {},
            onBackClick = {}
        )
    }
}
