package lk.moneytrail.app.presentation.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme

// Navigation shell: the setup steps come later.
@Composable
fun OnboardingScreen(
    onFinishClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to MoneyTrail",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Let's set up your account before you start tracking.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onFinishClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Finish setup")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    MoneyTrailTheme {
        OnboardingScreen(onFinishClick = {})
    }
}
