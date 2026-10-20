package lk.moneytrail.app.presentation.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lk.moneytrail.app.presentation.calendar.CalendarScreen
import lk.moneytrail.app.presentation.components.MoneyTrailIcons
import lk.moneytrail.app.presentation.folders.FoldersScreen
import lk.moneytrail.app.presentation.insights.InsightsScreen
import lk.moneytrail.app.presentation.more.MoreScreen
import java.time.LocalDate

private enum class DashboardTab(val label: String, val icon: ImageVector) {
    CALENDAR("Calendar", MoneyTrailIcons.Calendar),
    FOLDERS("Folders", MoneyTrailIcons.Folder),
    INSIGHTS("Insights", MoneyTrailIcons.Insights),
    MORE("More", MoneyTrailIcons.More)
}

/** The signed-in home: the bottom bar from the Figma screens, with the calendar first. */
@Composable
fun DashboardScreen(
    onAddEntryClick: (date: LocalDate) -> Unit,
    onEntryClick: (entryId: String) -> Unit,
    onSettingsClick: () -> Unit,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(DashboardTab.CALENDAR) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            DashboardNavigationBar(selected = selectedTab, onTabClick = { selectedTab = it })
        }
    ) { innerPadding ->
        val contentModifier = Modifier
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)
        when (selectedTab) {
            DashboardTab.CALENDAR -> CalendarScreen(
                onAddEntryClick = onAddEntryClick,
                onEntryClick = onEntryClick,
                modifier = contentModifier
            )
            DashboardTab.FOLDERS -> FoldersScreen(
                onAddEntryClick = { onAddEntryClick(LocalDate.now()) },
                modifier = contentModifier
            )
            DashboardTab.INSIGHTS -> InsightsScreen(modifier = contentModifier)
            DashboardTab.MORE -> MoreScreen(
                onSettingsClick = onSettingsClick,
                onSignedOut = onSignedOut,
                modifier = contentModifier
            )
        }
    }
}

@Composable
private fun DashboardNavigationBar(
    selected: DashboardTab,
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
                    icon = { Icon(tab.icon, contentDescription = null, modifier = Modifier.size(22.dp)) },
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
