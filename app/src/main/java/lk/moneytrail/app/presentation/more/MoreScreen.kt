package lk.moneytrail.app.presentation.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.moneytrail.app.presentation.components.MoneyTrailIcons
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme

// The start of MoneyTrail-Figma-HTML/12-More.html: the profile, Settings and sign-out.
// The money in, bills and account sections come with the features they open.
@Composable
fun MoreScreen(
    onSettingsClick: () -> Unit,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MoreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MoreContent(
        uiState = uiState,
        onRetry = viewModel::loadProfile,
        onSettingsClick = onSettingsClick,
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
            modifier = Modifier.padding(top = 22.dp, bottom = 4.dp)
        )

        when (uiState) {
            MoreState.Loading -> ProfileCard(title = "Loading your profile…", subtitle = null, initial = null)
            is MoreState.Error -> ProfileError(message = uiState.message, onRetry = onRetry)
            is MoreState.Ready -> ProfileCard(
                title = uiState.name?.takeIf { it.isNotBlank() } ?: "Your account",
                subtitle = uiState.email,
                initial = (uiState.name?.takeIf { it.isNotBlank() } ?: uiState.email)?.take(1)?.uppercase()
            )
        }

        MoreRow(
            icon = MoneyTrailIcons.Settings,
            title = "Settings",
            subtitle = "Salary, pay day and your categories",
            onClick = onSettingsClick
        )
        // Always shown, so the user can sign out even if the profile didn't load.
        MoreRow(icon = MoneyTrailIcons.SignOut, title = "Sign out", subtitle = null, onClick = onSignOutClick)
    }
}

@Composable
private fun MoreRow(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
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
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(title: String, subtitle: String?, initial: String?) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(colors.primaryContainer, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial.orEmpty(),
                style = MaterialTheme.typography.titleSmall,
                fontSize = 17.sp,
                color = colors.onPrimaryContainer
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.outline)
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
            uiState = MoreState.Ready(name = "Kavindu Silva", email = "kavindu.silva@gmail.com"),
            onRetry = {},
            onSettingsClick = {},
            onSignOutClick = {}
        )
    }
}
