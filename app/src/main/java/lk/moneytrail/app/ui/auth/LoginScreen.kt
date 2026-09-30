package lk.moneytrail.app.ui.auth

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.moneytrail.app.ui.theme.MoneyTrailTheme

@Composable
fun LoginScreen(
    onSignedIn: () -> Unit,
    onCreateAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSignedIn) {
        if (uiState.isSignedIn) onSignedIn()
    }

    LoginContent(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLoginClick = viewModel::signIn,
        onCreateAccountClick = onCreateAccountClick,
        modifier = modifier
    )
}

@Composable
private fun LoginContent(
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        onLoginClick()
    }

    AuthScreenLayout(modifier = modifier) {
        Text(
            text = "MoneyTrail",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = "Log in to keep track of your spending.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        EmailField(
            value = uiState.email,
            onValueChange = onEmailChange,
            enabled = !uiState.isLoading
        )
        PasswordField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            enabled = !uiState.isLoading,
            onDone = submit
        )
        if (uiState.errorMessage != null) {
            AuthErrorText(uiState.errorMessage)
        }
        AuthSubmitButton(
            text = "Log in",
            isLoading = uiState.isLoading,
            onClick = submit
        )
        TextButton(
            onClick = onCreateAccountClick,
            enabled = !uiState.isLoading
        ) {
            Text("Create an account")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    MoneyTrailTheme {
        LoginContent(
            uiState = LoginUiState(
                email = "nimal@example.com",
                errorMessage = "Email or password is incorrect."
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onLoginClick = {},
            onCreateAccountClick = {}
        )
    }
}
