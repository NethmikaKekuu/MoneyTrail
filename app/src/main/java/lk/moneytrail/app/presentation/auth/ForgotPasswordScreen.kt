package lk.moneytrail.app.presentation.auth

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.moneytrail.app.R
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme

// MoneyTrail-Figma-HTML/05-ForgotPassword.html
@Composable
fun ForgotPasswordScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ForgotPasswordContent(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onSendClick = viewModel::sendResetLink,
        onUseDifferentEmailClick = viewModel::onUseDifferentEmailClick,
        onBackClick = onBackClick,
        modifier = modifier
    )
}

@Composable
private fun ForgotPasswordContent(
    uiState: ForgotPasswordState,
    onEmailChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onUseDifferentEmailClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        onSendClick()
    }

    AuthScreenLayout(modifier = modifier) {
        AuthBackButton(
            onClick = onBackClick,
            contentDescription = "Back to log in",
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 16.dp, top = 18.dp)
        )

        Column(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            if (uiState.sentTo == null) {
                IconTile(R.drawable.ic_lock)
                TitleAndMessage(
                    title = "Forgot your password?",
                    message = "No problem. Enter the email you signed up with and we'll send you a reset link."
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EmailField(
                        value = uiState.email,
                        onValueChange = onEmailChange,
                        enabled = !uiState.isLoading,
                        imeAction = ImeAction.Done,
                        onDone = submit
                    )
                    if (uiState.errorMessage != null) {
                        AuthErrorText(uiState.errorMessage)
                    }
                }
                AuthPrimaryButton(
                    text = "Send reset link",
                    onClick = submit,
                    isLoading = uiState.isLoading
                )
            } else {
                IconTile(R.drawable.ic_mail)
                TitleAndMessage(
                    title = "Check your email",
                    message = "If ${uiState.sentTo} has a MoneyTrail account, a reset link is on its way. " +
                        "It can take a few minutes, so check your spam folder too."
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AuthPrimaryButton(text = "Back to log in", onClick = onBackClick)
                    TextButton(
                        onClick = onUseDifferentEmailClick,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Use a different email", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun IconTile(@DrawableRes icon: Int) {
    Box(
        modifier = Modifier
            .size(84.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(26.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(38.dp)
        )
    }
}

@Composable
private fun TitleAndMessage(title: String, message: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true, heightDp = 844)
@Composable
private fun ForgotPasswordScreenPreview() {
    MoneyTrailTheme {
        ForgotPasswordContent(
            uiState = ForgotPasswordState(email = "kavindu.silva@gmail.com"),
            onEmailChange = {},
            onSendClick = {},
            onUseDifferentEmailClick = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 844)
@Composable
private fun ForgotPasswordSentPreview() {
    MoneyTrailTheme {
        ForgotPasswordContent(
            uiState = ForgotPasswordState(
                email = "kavindu.silva@gmail.com",
                sentTo = "kavindu.silva@gmail.com"
            ),
            onEmailChange = {},
            onSendClick = {},
            onUseDifferentEmailClick = {},
            onBackClick = {}
        )
    }
}
