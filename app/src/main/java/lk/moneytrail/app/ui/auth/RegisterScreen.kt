package lk.moneytrail.app.ui.auth

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.moneytrail.app.ui.theme.MoneyTrailTheme

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onHaveAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isRegistered) {
        if (uiState.isRegistered) onRegistered()
    }

    RegisterContent(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onCreateAccountClick = viewModel::register,
        onHaveAccountClick = onHaveAccountClick,
        modifier = modifier
    )
}

@Composable
private fun RegisterContent(
    uiState: RegisterUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onCreateAccountClick: () -> Unit,
    onHaveAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        onCreateAccountClick()
    }

    AuthScreenLayout(modifier = modifier) {
        Text(
            text = "Create your account",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Start tracking where your money goes.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        NameField(
            value = uiState.name,
            onValueChange = onNameChange,
            enabled = !uiState.isLoading
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
            label = "Password (at least $MIN_PASSWORD_LENGTH characters)",
            imeAction = ImeAction.Next
        )
        PasswordField(
            value = uiState.confirmPassword,
            onValueChange = onConfirmPasswordChange,
            enabled = !uiState.isLoading,
            label = "Confirm password",
            onDone = submit
        )
        if (uiState.errorMessage != null) {
            AuthErrorText(uiState.errorMessage)
        }
        AuthSubmitButton(
            text = "Create account",
            isLoading = uiState.isLoading,
            onClick = submit
        )
        TextButton(
            onClick = onHaveAccountClick,
            enabled = !uiState.isLoading
        ) {
            Text("I already have an account")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RegisterScreenPreview() {
    MoneyTrailTheme {
        RegisterContent(
            uiState = RegisterUiState(name = "Nimal Perera", email = "nimal@example.com"),
            onNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onCreateAccountClick = {},
            onHaveAccountClick = {}
        )
    }
}
