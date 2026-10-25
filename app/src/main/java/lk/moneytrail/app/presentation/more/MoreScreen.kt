package lk.moneytrail.app.presentation.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.moneytrail.app.data.preferences.ThemeMode
import lk.moneytrail.app.presentation.components.MoneyTrailIcons
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme

@Composable
fun MoreScreen(
    onSettingsClick: () -> Unit,
    onConfirmSalaryClick: () -> Unit,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MoreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MoreContent(
        uiState = uiState,
        onRetry = viewModel::loadProfile,
        onSettingsClick = onSettingsClick,
        onConfirmSalaryClick = onConfirmSalaryClick,
        onOpenThemeDialog = viewModel::onOpenThemeDialog,
        onSelectThemeMode = viewModel::onSelectThemeMode,
        onDismissThemeDialog = viewModel::onDismissThemeDialog,
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
    onRetry: () -> Unit,
    onSettingsClick: () -> Unit,
    onConfirmSalaryClick: () -> Unit,
    onOpenThemeDialog: () -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onDismissThemeDialog: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
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
            icon = MoneyTrailIcons.Check,
            title = "Confirm Salary",
            subtitle = "Confirm payday arrival & bonus",
            badgeText = (uiState as? MoreState.Ready)?.salaryBadge,
            onClick = onConfirmSalaryClick
        )

        val currentThemeMode = (uiState as? MoreState.Ready)?.themeMode ?: ThemeMode.SYSTEM
        MoreRow(
            icon = MoneyTrailIcons.Circle,
            title = "App Theme",
            subtitle = "Light, Dark, or System Default",
            badgeText = currentThemeMode.label,
            onClick = onOpenThemeDialog
        )

        MoreRow(
            icon = MoneyTrailIcons.Settings,
            title = "Settings",
            subtitle = "Salary, pay day and your categories",
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

    if ((uiState as? MoreState.Ready)?.isThemeDialogOpen == true) {
        val readyState = uiState as MoreState.Ready
        ThemeSelectionDialog(
            selectedMode = readyState.themeMode,
            onSelectMode = onSelectThemeMode,
            onDismiss = onDismissThemeDialog
        )
    }
}

@Composable
private fun ThemeSelectionDialog(
    selectedMode: ThemeMode,
    onSelectMode: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select App Theme",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ThemeMode.entries.forEach { mode ->
                    val isSelected = mode == selectedMode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .selectable(
                                selected = isSelected,
                                role = Role.RadioButton,
                                onClick = { onSelectMode(mode) }
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null
                        )
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
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
                email = "kavindu.silva@gmail.com",
                salaryBadge = "Due 25 Oct",
                themeMode = ThemeMode.SYSTEM
            ),
            onRetry = {},
            onSettingsClick = {},
            onConfirmSalaryClick = {},
            onOpenThemeDialog = {},
            onSelectThemeMode = {},
            onDismissThemeDialog = {},
            onSignOutClick = {}
        )
    }
}
