package lk.moneytrail.app.presentation.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.moneytrail.app.domain.model.NotificationKind
import lk.moneytrail.app.presentation.calendar.CalendarScreen
import lk.moneytrail.app.presentation.components.MoneyTrailIcons
import lk.moneytrail.app.presentation.components.OfflineBanner
import lk.moneytrail.app.presentation.folders.FoldersScreen
import lk.moneytrail.app.presentation.goal.GoalScreen
import lk.moneytrail.app.presentation.insights.InsightsScreen
import lk.moneytrail.app.presentation.more.MoreScreen
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme
import java.time.LocalDate

private enum class DashboardTab(val label: String, val icon: ImageVector) {
    CALENDAR("Calendar", MoneyTrailIcons.Calendar),
    FOLDERS("Folders", MoneyTrailIcons.Folder),
    GOAL("Goal", MoneyTrailIcons.Goal),
    INSIGHTS("Insights", MoneyTrailIcons.Insights),
    MORE("More", MoneyTrailIcons.More)
}

/**
 * The signed-in home: the bottom bar from the Figma screens, with the calendar first.
 * While the phone is offline, a banner above the tabs says the app is showing what's
 * saved on the phone.
 */
@Composable
fun DashboardScreen(
    onAddEntryClick: (date: LocalDate) -> Unit,
    onEntryClick: (entryId: String) -> Unit,
    onSettingsClick: () -> Unit,
    onSalaryClick: () -> Unit,
    onCatchUpClick: () -> Unit,
    onStatementClick: () -> Unit,
    onFreelanceClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Back after a break: start with the catch-up.
    LaunchedEffect(uiState.openCatchUp) {
        if (uiState.openCatchUp) {
            viewModel.onCatchUpOpened()
            onCatchUpClick()
        }
    }
    var selectedTab by rememberSaveable { mutableStateOf(DashboardTab.CALENDAR) }
    var checkDueRequested by remember { mutableStateOf(false) }
    // Back from another tab goes to the calendar first, like the bottom bars of other apps.
    BackHandler(enabled = selectedTab != DashboardTab.CALENDAR) { selectedTab = DashboardTab.CALENDAR }

    // Back on the dashboard: reminders that were dealt with leave the notification list.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }

    // "Check now" or "Open savings" chosen in the notification list.
    LaunchedEffect(uiState.notificationAction) {
        when (uiState.notificationAction) {
            NotificationKind.MONEY_DUE -> {
                selectedTab = DashboardTab.CALENDAR
                checkDueRequested = true
            }
            NotificationKind.PAY_YOURSELF_FIRST -> selectedTab = DashboardTab.GOAL
            NotificationKind.CATCH_UP -> onCatchUpClick()
            NotificationKind.INVOICE_OVERDUE -> onFreelanceClick()
            null -> return@LaunchedEffect
        }
        viewModel.onNotificationActionHandled()
    }

    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        // The app is open, so the cards show what the reminders said.
        NotificationManagerCompat.from(context).cancelAll()
        // Android 13 and later ask before an app may show notifications.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            DashboardNavigationBar(
                selected = selectedTab,
                unreadNotifications = uiState.unreadNotifications,
                onTabClick = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            AnimatedVisibility(visible = uiState.isOffline) {
                OfflineBanner()
            }


            Box(Modifier.weight(1f)) {
                val tabModifier = Modifier.fillMaxSize()
                when (selectedTab) {
                    DashboardTab.CALENDAR -> CalendarScreen(
                        onAddEntryClick = onAddEntryClick,
                        onEntryClick = onEntryClick,
                        onPayYourselfFirstClick = { selectedTab = DashboardTab.GOAL },
                        unreadNotifications = uiState.unreadNotifications,
                        onNotificationsClick = onNotificationsClick,
                        checkDueRequested = checkDueRequested,
                        onCheckDueRequestHandled = { checkDueRequested = false },
                        modifier = tabModifier
                    )
                    DashboardTab.FOLDERS -> FoldersScreen(
                        onAddEntryClick = { onAddEntryClick(LocalDate.now()) },
                        modifier = tabModifier
                    )
                    DashboardTab.GOAL -> GoalScreen(
                        onBackClick = { selectedTab = DashboardTab.CALENDAR },
                        modifier = tabModifier
                    )
                    DashboardTab.INSIGHTS -> InsightsScreen(modifier = tabModifier)
                    DashboardTab.MORE -> MoreScreen(
                        unreadNotifications = uiState.unreadNotifications,
                        onNotificationsClick = onNotificationsClick,
                        onSettingsClick = onSettingsClick,
                        onSalaryClick = onSalaryClick,
                        onCatchUpClick = onCatchUpClick,
                        onStatementClick = onStatementClick,
                        onFreelanceClick = onFreelanceClick,
                        onSignedOut = onSignedOut,
                        modifier = tabModifier
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardNavigationBar(
    selected: DashboardTab,
    unreadNotifications: Int,
    onTabClick: (DashboardTab) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column {
        HorizontalDivider(color = colors.outlineVariant)
        NavigationBar(containerColor = colors.surface, tonalElevation = 0.dp) {
            DashboardTab.entries.forEach { tab ->
                val isSelected = tab == selected
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onTabClick(tab) },
                    icon = {
                        // Waiting reminders show on More, where the notification list now lives.
                        BadgedBox(badge = {
                            if (tab == DashboardTab.MORE && unreadNotifications > 0) {
                                Badge { Text(if (unreadNotifications > 9) "9+" else unreadNotifications.toString()) }
                            }
                        }) {
                            Icon(tab.icon, contentDescription = null, modifier = Modifier.size(22.dp))
                        }
                    },
                    label = {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.primary,
                        selectedTextColor = colors.primary,
                        indicatorColor = colors.primaryContainer,
                        unselectedIconColor = colors.outline,
                        unselectedTextColor = colors.outline
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardNavigationBarPreview() {
    MoneyTrailTheme {
        DashboardNavigationBar(selected = DashboardTab.CALENDAR, unreadNotifications = 3, onTabClick = {})
    }
}
