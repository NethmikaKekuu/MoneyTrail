package lk.moneytrail.app.presentation.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import lk.moneytrail.app.R
import lk.moneytrail.app.presentation.components.MoneyTrailLogo
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme

// MoneyTrail-Figma-HTML/03-Login.html
@Composable
fun LoginScreen(
    onSignedIn: () -> Unit,
    onNewGoogleUser: () -> Unit,
    onCreateAccountClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val webClientId = stringResource(R.string.default_web_client_id)
    val scope = rememberCoroutineScope()
    // True while Google's account sheet is open. Screen-only: it closes with the screen.
    var isPickingGoogleAccount by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.signedIn) {
        when (uiState.signedIn) {
            SignedIn.RETURNING_USER -> onSignedIn()
            SignedIn.NEW_USER -> onNewGoogleUser()
            null -> Unit
        }
    }

    LoginContent(
        uiState = uiState,
        isPickingGoogleAccount = isPickingGoogleAccount,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLoginClick = viewModel::signIn,
        onGoogleClick = {
            isPickingGoogleAccount = true
            scope.launch {
                val result = requestGoogleIdToken(context, webClientId)
                isPickingGoogleAccount = false
                viewModel.onGoogleIdTokenResult(result)
            }
        },
        onForgotPasswordClick = onForgotPasswordClick,
        onCreateAccountClick = onCreateAccountClick,
        modifier = modifier
    )
}

@Composable
private fun LoginContent(
    uiState: LoginState,
    isPickingGoogleAccount: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onGoogleClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        onLoginClick()
    }
    val isBusy = uiState.isBusy || isPickingGoogleAccount

    AuthScreenLayout(modifier = modifier) {
        LoginHeader()

        // The form sheet overlaps the bottom of the orange header.
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-28).dp)
        ) {
            Column(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EmailField(
                    value = uiState.email,
                    onValueChange = onEmailChange,
                    enabled = !isBusy
                )
                PasswordField(
                    value = uiState.password,
                    onValueChange = onPasswordChange,
                    enabled = !isBusy,
                    placeholder = "Your password",
                    onDone = submit,
                    labelAction = {
                        AuthLink(
                            text = "Forgot password?",
                            onClick = onForgotPasswordClick,
                            enabled = !isBusy
                        )
                    }
                )
                if (uiState.errorMessage != null) {
                    AuthErrorText(uiState.errorMessage)
                }
                AuthPrimaryButton(
                    text = "Log in",
                    onClick = submit,
                    isLoading = uiState.isLoading,
                    enabled = !isBusy
                )
                OrDivider()
                GoogleSignInButton(
                    onClick = onGoogleClick,
                    isLoading = uiState.isGoogleLoading,
                    enabled = !isBusy
                )
            }
        }

        Spacer(Modifier.weight(1f))
        AuthFooter(
            prompt = "New to MoneyTrail?",
            actionText = "Create an account",
            onActionClick = onCreateAccountClick,
            enabled = !isBusy,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun LoginHeader() {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.primary)
    ) {
        // Decorative rings in the top-right corner, as in the design.
        Canvas(Modifier.matchParentSize()) {
            val center = Offset(size.width - 40.dp.toPx(), 60.dp.toPx())
            drawCircle(Color.White.copy(alpha = 0.05f), radius = 110.dp.toPx(), center = center)
            drawCircle(
                Color.White.copy(alpha = 0.10f),
                radius = 170.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 54.dp)
        ) {
            MoneyTrailLogo(size = 52.dp, reversed = true)
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Welcome back",
                style = MaterialTheme.typography.headlineLarge,
                color = colors.onPrimary
            )
            Text(
                text = "Log in to pick up your trail.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onPrimary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 844)
@Composable
private fun LoginScreenPreview() {
    MoneyTrailTheme {
        LoginContent(
            uiState = LoginState(
                email = "kavindu.silva@gmail.com",
                errorMessage = "Email or password is incorrect."
            ),
            isPickingGoogleAccount = false,
            onEmailChange = {},
            onPasswordChange = {},
            onLoginClick = {},
            onGoogleClick = {},
            onForgotPasswordClick = {},
            onCreateAccountClick = {}
        )
    }
}
