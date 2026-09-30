package lk.moneytrail.app.ui.dashboard

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.moneytrail.app.ui.components.EmptyView
import lk.moneytrail.app.ui.components.ErrorView
import lk.moneytrail.app.ui.components.LoadingView
import lk.moneytrail.app.ui.theme.MoneyTrailTheme

@Composable
fun DashboardScreen(
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DashboardContent(
        uiState = uiState,
        onRetry = viewModel::loadProfile,
        onSignOutClick = {
            viewModel.signOut()
            onSignedOut()
        },
        modifier = modifier
    )
}

// Placeholder until balances and transactions are built.
@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onRetry: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        bottomBar = {
            TextButton(
                onClick = onSignOutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(8.dp)
            ) {
                Text("Sign out")
            }
        }
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (uiState) {
            DashboardUiState.Loading -> LoadingView(
                message = "Loading your profile…",
                modifier = contentModifier
            )
            is DashboardUiState.Error -> ErrorView(
                message = uiState.message,
                onRetry = onRetry,
                modifier = contentModifier
            )
            is DashboardUiState.Ready -> EmptyView(
                title = if (uiState.name.isNullOrBlank()) "Welcome" else "Hi, ${uiState.name}",
                message = listOfNotNull(
                    "Your dashboard is coming soon.",
                    uiState.email?.let { "Signed in as $it" }
                ).joinToString("\n"),
                modifier = contentModifier
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    MoneyTrailTheme {
        DashboardContent(
            uiState = DashboardUiState.Ready(name = "Nimal", email = "nimal@example.com"),
            onRetry = {},
            onSignOutClick = {}
        )
    }
}
