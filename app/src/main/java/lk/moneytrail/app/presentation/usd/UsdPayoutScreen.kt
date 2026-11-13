package lk.moneytrail.app.presentation.usd

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.moneytrail.app.domain.model.UsdConversion
import lk.moneytrail.app.domain.repository.MarketRate
import lk.moneytrail.app.presentation.auth.AuthBackButton
import lk.moneytrail.app.presentation.auth.AuthErrorText
import lk.moneytrail.app.presentation.auth.AuthPrimaryButton
import lk.moneytrail.app.presentation.auth.AuthTextField
import lk.moneytrail.app.presentation.components.MoneyTrailDatePickerDialog
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DayFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)
private val ShortDayFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

/** Log an AdSense payment in dollars: the market rate is suggested and the rate the bank really gave is worked out. */
@Composable
fun UsdPayoutScreen(
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UsdPayoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaved()
    }

    UsdPayoutContent(
        uiState = uiState,
        onUsdChange = viewModel::onUsdChange,
        onLkrChange = viewModel::onLkrChange,
        onDateChange = viewModel::onDateChange,
        onTypeOwnRateClick = viewModel::onTypeOwnRateClick,
        onUseSuggestedClick = viewModel::onUseSuggestedClick,
        onOwnRateChange = viewModel::onOwnRateChange,
        onRetryRateClick = viewModel::onRetryRateClick,
        onSaveClick = viewModel::save,
        onBackClick = onBackClick,
        modifier = modifier
    )
}

@Composable
private fun UsdPayoutContent(
    uiState: UsdPayoutState,
    onUsdChange: (String) -> Unit,
    onLkrChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onTypeOwnRateClick: () -> Unit,
    onUseSuggestedClick: () -> Unit,
    onOwnRateChange: (String) -> Unit,
    onRetryRateClick: () -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var isDatePickerOpen by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AuthBackButton(onClick = onBackClick, contentDescription = "Back", modifier = Modifier.align(Alignment.CenterStart))
            Text("AdSense payment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
            AuthTextField(
                value = uiState.usdText,
                onValueChange = onUsdChange,
                label = "Amount (USD)",
                placeholder = "0.00",
                enabled = !uiState.isSaving,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                modifier = Modifier.weight(1.2f)
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Received", style = MaterialTheme.typography.labelLarge)
                DateField(uiState.date, onClick = { isDatePickerOpen = true })
            }
        }

        SuggestedRateCard(
            uiState = uiState,
            onTypeOwnRateClick = onTypeOwnRateClick,
            onUseSuggestedClick = onUseSuggestedClick,
            onOwnRateChange = onOwnRateChange,
            onRetryClick = onRetryRateClick
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AuthTextField(
                value = uiState.lkrText,
                onValueChange = onLkrChange,
                label = "LKR credited to ${uiState.accountName}",
                placeholder = "0",
                enabled = !uiState.isSaving,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
            )
            Text(
                text = if (uiState.isLkrAutomatic) "Worked out from the rate. If your bank credited a different amount, type it."
                else "Typed by you. Clear it to use the rate again.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }

        Text(
            text = "Saved values never change, even if rates move later.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant
        )

        uiState.errorMessage?.let { AuthErrorText(it) }

        Spacer(Modifier.height(8.dp))
        AuthPrimaryButton(
            text = "Save to Income › AdSense",
            onClick = onSaveClick,
            isLoading = uiState.isSaving,
            enabled = uiState.canSave
        )
    }

    if (isDatePickerOpen) {
        MoneyTrailDatePickerDialog(
            date = uiState.date,
            onDateSelected = onDateChange,
            onDismiss = { isDatePickerOpen = false }
        )
    }
}

@Composable
private fun DateField(date: LocalDate, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(colors.surface)
            .border(BorderStroke(1.dp, colors.outlineVariant), shape)
            .clickable(onClickLabel = "Change the date", onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(date.format(DayFormatter), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SuggestedRateCard(
    uiState: UsdPayoutState,
    onTypeOwnRateClick: () -> Unit,
    onUseSuggestedClick: () -> Unit,
    onOwnRateChange: (String) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceContainerLow)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (uiState.isOwnRate) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Your market rate (LKR per USD)",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (uiState.suggested is SuggestedRate.Found) {
                    TextButton(onClick = onUseSuggestedClick) { Text("Use suggested") }
                }
            }
            AuthTextField(
                value = uiState.ownRateText,
                onValueChange = onOwnRateChange,
                label = "1 USD =",
                placeholder = "e.g. 299.40",
                enabled = !uiState.isSaving,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
            )
            (uiState.suggested as? SuggestedRate.Failed)?.let {
                Text(it.message, style = MaterialTheme.typography.bodySmall, color = colors.error)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    when (val suggested = uiState.suggested) {
                        is SuggestedRate.Found -> {
                            Text(
                                text = "Suggested market rate, ${suggested.rate.date.format(ShortDayFormatter)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                            Text(
                                text = "1 USD = ${UsdConversion.formatHundredths(suggested.rate.rate)} LKR",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        is SuggestedRate.Loading -> Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.width(18.dp).height(18.dp), strokeWidth = 2.dp)
                            Text("Getting the market rate…", style = MaterialTheme.typography.bodyMedium)
                        }
                        is SuggestedRate.Failed -> {
                            Text(suggested.message, style = MaterialTheme.typography.bodySmall, color = colors.error)
                            TextButton(onClick = onRetryClick) { Text("Try again") }
                        }
                    }
                }
                TextButton(onClick = onTypeOwnRateClick) {
                    Text("Type my own", color = colors.tertiary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun UsdPayoutScreenPreview() {
    MoneyTrailTheme {
        UsdPayoutContent(
            uiState = UsdPayoutState(
                usdText = "38.00",
                date = LocalDate.of(2026, 10, 8),
                                suggested = SuggestedRate.Found(MarketRate(29_940, LocalDate.of(2026, 10, 8)))
            ),
            onUsdChange = {},
            onLkrChange = {},
            onDateChange = {},
            onTypeOwnRateClick = {},
            onUseSuggestedClick = {},
            onOwnRateChange = {},
            onRetryRateClick = {},
            onSaveClick = {},
            onBackClick = {}
        )
    }
}
