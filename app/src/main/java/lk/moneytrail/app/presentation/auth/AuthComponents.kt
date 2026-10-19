package lk.moneytrail.app.presentation.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import lk.moneytrail.app.R

// Building blocks for the auth screens, styled after MoneyTrail-Figma-HTML/03 to 05.

private val FieldShape = RoundedCornerShape(16.dp)
private val ButtonShape = RoundedCornerShape(18.dp)
private val ButtonHeight = 54.dp

/**
 * Full-screen scrollable column that stays above the keyboard. Its content is at
 * least as tall as the screen, so a `Spacer(Modifier.weight(1f))` can push a footer
 * to the bottom. Screens add their own status bar padding, so orange headers can
 * reach the top edge.
 */
@Composable
internal fun AuthScreenLayout(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .fillMaxWidth(),
            content = content
        )
    }
}

/** Text field with its label above it, and an optional link at the end of the label row. */
@Composable
internal fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    labelAction: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            labelAction?.invoke()
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = FieldShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surfaceContainerLow,
                unfocusedContainerColor = colors.surfaceContainerLow,
                disabledContainerColor = colors.surfaceContainerLow,
                focusedBorderColor = colors.tertiary,
                unfocusedBorderColor = colors.outlineVariant,
                disabledBorderColor = colors.outlineVariant,
                focusedPlaceholderColor = colors.outline,
                unfocusedPlaceholderColor = colors.outline
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
internal fun NameField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    AuthTextField(
        value = value,
        onValueChange = onValueChange,
        label = "Your name",
        placeholder = "First name",
        enabled = enabled,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Next
        ),
        modifier = modifier
    )
}

@Composable
internal fun EmailField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
    onDone: () -> Unit = {}
) {
    AuthTextField(
        value = value,
        onValueChange = onValueChange,
        label = "Email",
        placeholder = "you@example.com",
        enabled = enabled,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier
    )
}

/** Password field with a show/hide button. Whether it's shown is screen-only state. */
@Composable
internal fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    placeholder: String,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Done,
    onDone: () -> Unit = {},
    labelAction: @Composable (() -> Unit)? = null
) {
    var isVisible by rememberSaveable { mutableStateOf(false) }
    AuthTextField(
        value = value,
        onValueChange = onValueChange,
        label = "Password",
        placeholder = placeholder,
        enabled = enabled,
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        labelAction = labelAction,
        trailingIcon = {
            IconButton(onClick = { isVisible = !isVisible }) {
                Icon(
                    painter = painterResource(
                        if (isVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility
                    ),
                    contentDescription = if (isVisible) "Hide password" else "Show password",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        modifier = modifier
    )
}

/** Small bold link, e.g. "Forgot password?" next to a field label. */
@Composable
internal fun AuthLink(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = 4.dp)
    )
}

/** "New to MoneyTrail? Create an account" style line at the bottom of a screen. */
@Composable
internal fun AuthFooter(
    prompt: String,
    actionText: String,
    onActionClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$prompt ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = actionText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable(enabled = enabled, role = Role.Button, onClick = onActionClick)
                .padding(vertical = 12.dp)
        )
    }
}

@Composable
internal fun AuthErrorText(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
internal fun AuthPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = ButtonShape,
        // Keep the button orange while it's busy, so the spinner reads as progress.
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = colors.primary.copy(alpha = 0.6f),
            disabledContentColor = colors.onPrimary
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(ButtonHeight)
    ) {
        if (isLoading) {
            ButtonSpinner()
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Cream button for the second choice, e.g. "I already have an account". */
@Composable
internal fun AuthSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(ButtonHeight)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
internal fun GoogleSignInButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = ButtonShape,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(ButtonHeight)
    ) {
        if (isLoading) {
            ButtonSpinner()
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_google),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Continue with Google",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
    }
}

@Composable
internal fun AuthBackButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    OutlinedIconButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.size(44.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_chevron_left),
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
internal fun OrDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            text = "or",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun ButtonSpinner() {
    CircularProgressIndicator(
        modifier = Modifier.size(20.dp),
        color = LocalContentColor.current,
        strokeWidth = 2.dp
    )
}
