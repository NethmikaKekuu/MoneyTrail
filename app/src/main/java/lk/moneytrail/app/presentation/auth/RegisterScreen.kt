package lk.moneytrail.app.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.moneytrail.app.presentation.components.MoneyTrailIcons
import lk.moneytrail.app.presentation.components.MoneyTrailLogo
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme

// MoneyTrail-Figma-HTML/04-SignUp.html
@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onBackClick: () -> Unit,
    onLogInClick: () -> Unit,
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
        onTermsCheckedChange = viewModel::onTermsCheckedChange,
        onCreateAccountClick = viewModel::register,
        onBackClick = onBackClick,
        onLogInClick = onLogInClick,
        modifier = modifier
    )
}

@Composable
private fun RegisterContent(
    uiState: RegisterState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTermsCheckedChange: (Boolean) -> Unit,
    onCreateAccountClick: () -> Unit,
    onBackClick: () -> Unit,
    onLogInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        onCreateAccountClick()
    }
    val enabled = !uiState.isLoading

    AuthScreenLayout(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AuthBackButton(onClick = onBackClick, contentDescription = "Back")
            MoneyTrailLogo(size = 36.dp)
            Spacer(Modifier.size(44.dp))
        }

        Column(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Create your account", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Takes under a minute. No bank login, ever.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            NameField(value = uiState.name, onValueChange = onNameChange, enabled = enabled)
            EmailField(value = uiState.email, onValueChange = onEmailChange, enabled = enabled)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PasswordField(
                    value = uiState.password,
                    onValueChange = onPasswordChange,
                    enabled = enabled,
                    placeholder = "At least $MIN_PASSWORD_LENGTH characters",
                    onDone = { focusManager.clearFocus() }
                )
                PasswordStrength(uiState.passwordChecks)
            }
            TermsCheckbox(
                checked = uiState.hasAcceptedTerms,
                onCheckedChange = onTermsCheckedChange,
                enabled = enabled
            )
        }

        Spacer(Modifier.weight(1f))
        Column(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (uiState.errorMessage != null) {
                AuthErrorText(uiState.errorMessage, Modifier.padding(bottom = 8.dp))
            }
            AuthPrimaryButton(
                text = "Create account",
                onClick = submit,
                isLoading = uiState.isLoading
            )
            AuthFooter(
                prompt = "Already have an account?",
                actionText = "Log in",
                onActionClick = onLogInClick,
                enabled = enabled
            )
        }
    }
}

/** Three bars that fill as the password passes each rule, then the rules themselves. */
@Composable
private fun PasswordStrength(checks: PasswordChecks) {
    val colors = MaterialTheme.colorScheme
    val strength = when (checks.passedCount) {
        0 -> "Too short"
        1 -> "Weak"
        2 -> "Fair"
        else -> "Strong"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Password strength: $strength" },
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(3) { index ->
            Spacer(
                Modifier
                    .weight(1f)
                    .height(5.dp)
                    .background(
                        color = if (index < checks.passedCount) colors.primary else colors.outlineVariant,
                        shape = RoundedCornerShape(3.dp)
                    )
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PasswordRule("At least $MIN_PASSWORD_LENGTH characters", checks.isLongEnough)
        PasswordRule("Includes a number", checks.hasNumber)
        PasswordRule("Includes a letter", checks.hasLetter)
    }
}

@Composable
private fun PasswordRule(text: String, isMet: Boolean) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = if (isMet) MoneyTrailIcons.Check else MoneyTrailIcons.Circle,
            contentDescription = if (isMet) "Done:" else "Not yet:",
            tint = if (isMet) colors.primary else colors.outlineVariant,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (isMet) colors.onSurface else colors.outline
        )
    }
}

@Composable
private fun TermsCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        Text(
            text = buildAnnotatedString {
                append("I agree to the ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Terms") }
                append(" and ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Privacy Policy") }
                append(". My data stays in my account only.")
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 10.dp, top = 4.dp, bottom = 4.dp)
        )
    }
}

@Preview(showBackground = true, heightDp = 844)
@Composable
private fun RegisterScreenPreview() {
    MoneyTrailTheme {
        RegisterContent(
            uiState = RegisterState(
                name = "Kavindu",
                email = "kavindu.silva@gmail.com",
                password = "trail20"
            ),
            onNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onTermsCheckedChange = {},
            onCreateAccountClick = {},
            onBackClick = {},
            onLogInClick = {}
        )
    }
}
