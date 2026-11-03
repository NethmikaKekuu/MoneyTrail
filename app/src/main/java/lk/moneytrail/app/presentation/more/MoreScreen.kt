package lk.moneytrail.app.presentation.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.moneytrail.app.presentation.components.MoneyTrailIcons
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme

@Composable
fun MoreScreen(
    unreadNotifications: Int,
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSalaryClick: () -> Unit,
    onCatchUpClick: () -> Unit,
    onStatementClick: () -> Unit,
    onFreelanceClick: () -> Unit,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MoreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MoreContent(
        uiState = uiState,
        unreadNotifications = unreadNotifications,
        onNotificationsClick = onNotificationsClick,
        onRetry = viewModel::loadProfile,
        onSettingsClick = onSettingsClick,
        onSalaryClick = onSalaryClick,
        onCatchUpClick = onCatchUpClick,
        onStatementClick = onStatementClick,
        onFreelanceClick = onFreelanceClick,
        onSignOutClick = {
            viewModel.signOut()
            onSignedOut()
        },
        modifier = modifier
    )
}

@Composable
private fun MoreContent(
    uiState: MoreState,
    unreadNotifications: Int,
    onNotificationsClick: () -> Unit,
    onRetry: () -> Unit,
    onSettingsClick: () -> Unit,
    onSalaryClick: () -> Unit,
    onCatchUpClick: () -> Unit,
    onStatementClick: () -> Unit,
    onFreelanceClick: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "More",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 22.dp, bottom = 4.dp)
        )

        // Profile Section
        when (uiState) {
            MoreState.Loading -> ProfileCard(title = "Loading your profile…", subtitle = null, initial = null)
            is MoreState.Error -> ProfileError(message = uiState.message, onRetry = onRetry)
            is MoreState.Ready -> ProfileCard(
                title = uiState.name?.takeIf { it.isNotBlank() } ?: "Your account",
                subtitle = uiState.email,
                initial = (uiState.name?.takeIf { it.isNotBlank() } ?: uiState.email)?.take(1)?.uppercase()
            )
        }

        // Core Actionable Rows
        MoreRow(
            icon = MoneyTrailIcons.Bell,
            title = "Notifications",
            subtitle = when (unreadNotifications) {
                0 -> "You're all caught up"
                1 -> "1 reminder waiting"
                else -> "$unreadNotifications reminders waiting"
            },
            onClick = onNotificationsClick
        )

        MoreRow(
            icon = MoneyTrailIcons.Card,
            title = "Freelance projects",
            subtitle = "Track clients, milestone invoices & payments",
            onClick = onFreelanceClick
        )

        MoreRow(
            icon = MoneyTrailIcons.Check,
            title = "Salary",
            subtitle = "Pay day and base salary",
            onClick = onSalaryClick
        )

        MoreRow(
            icon = MoneyTrailIcons.Calendar,
            title = "Catch up",
            subtitle = "Back after a break? Get current and balance your accounts",
            onClick = onCatchUpClick
        )

        MoreRow(
            icon = MoneyTrailIcons.Note,
            title = "Monthly close & statement",
            subtitle = "Check each account against the bank and export the month as CSV",
            onClick = onStatementClick
        )

        MoreRow(
            icon = MoneyTrailIcons.Settings,
            title = "Settings",
            subtitle = "Theme and your categories",
            onClick = onSettingsClick
        )

        MoreRow(
            icon = MoneyTrailIcons.SignOut,
            title = "Sign out",
            subtitle = "Sign out of your account",
            iconTint = MaterialTheme.colorScheme.error,
            titleColor = MaterialTheme.colorScheme.error,
            onClick = onSignOutClick
        )
    }
}

@Composable
private fun MoreRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    badgeText: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    titleColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 60.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = iconTint)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (badgeText != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(title: String, subtitle: String?, initial: String?) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(colors.primaryContainer, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = colors.onPrimaryContainer
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.outline)
                }
            }
        }
    }
}

@Composable
private fun ProfileError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onRetry) { Text("Try again") }
    }
}

@Preview(showBackground = true)
@Composable
private fun MoreScreenPreview() {
    MoneyTrailTheme {
        MoreContent(
            uiState = MoreState.Ready(
                name = "Kavindu Silva",
                email = "kavindu.silva@gmail.com"
            ),
            unreadNotifications = 2,
            onNotificationsClick = {},
            onRetry = {},
            onSettingsClick = {},
            onSalaryClick = {},
            onCatchUpClick = {},
            onStatementClick = {},
            onFreelanceClick = {},
            onSignOutClick = {}
        )
    }
}
