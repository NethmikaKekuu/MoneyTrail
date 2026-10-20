package lk.moneytrail.app.presentation.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.moneytrail.app.domain.model.AccountKind
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.IncomeSource
import lk.moneytrail.app.presentation.auth.AuthBackButton
import lk.moneytrail.app.presentation.auth.AuthErrorText
import lk.moneytrail.app.presentation.auth.AuthPrimaryButton
import lk.moneytrail.app.presentation.auth.AuthTextField
import lk.moneytrail.app.presentation.components.AddCategoryDialog
import lk.moneytrail.app.presentation.components.MoneyTrailDatePickerDialog
import lk.moneytrail.app.presentation.components.MoneyTrailIcons
import lk.moneytrail.app.presentation.components.formatAmount
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Step 1 is MoneyTrail-Figma-HTML/06-Onboarding.html; steps 2 to 4 follow its style.
@Composable
fun OnboardingScreen(
    onFinishClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) onFinishClick()
    }

    // On steps 2 to 4, Back goes to the previous step instead of leaving the app.
    BackHandler(enabled = uiState.step != OnboardingStep.MONEY_PLACES) {
        viewModel.onBackClick()
    }

    OnboardingContent(
        uiState = uiState,
        onBackClick = viewModel::onBackClick,
        onSkipClick = onFinishClick,
        onContinueClick = viewModel::onContinueClick,
        onAccountToggle = viewModel::onAccountToggle,
        onAddPlaceClick = viewModel::onAddPlaceClick,
        onAddPlace = viewModel::onAddPlace,
        onAddPlaceDismiss = viewModel::onAddPlaceDismiss,
        onPayDayClick = viewModel::onPayDayClick,
        onBaseSalaryChange = viewModel::onBaseSalaryChange,
        onIncomeSourceToggle = viewModel::onIncomeSourceToggle,
        onOwnIncomeToggle = viewModel::onOwnIncomeToggle,
        onAddIncomeClick = viewModel::onAddIncomeClick,
        onAddIncome = viewModel::onAddIncome,
        onAddIncomeDismiss = viewModel::onAddIncomeDismiss,
        onGoalNameChange = viewModel::onGoalNameChange,
        onGoalTargetChange = viewModel::onGoalTargetChange,
        onGoalSavedChange = viewModel::onGoalSavedChange,
        onGoalDateChange = viewModel::onGoalDateChange,
        modifier = modifier
    )
}

private val GoalDateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)
private val GoalMonthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

@Composable
private fun OnboardingContent(
    uiState: OnboardingState,
    onBackClick: () -> Unit,
    onSkipClick: () -> Unit,
    onContinueClick: () -> Unit,
    onAccountToggle: (String) -> Unit,
    onAddPlaceClick: () -> Unit,
    onAddPlace: (String, AccountKind) -> Unit,
    onAddPlaceDismiss: () -> Unit,
    onPayDayClick: (Int) -> Unit,
    onBaseSalaryChange: (String) -> Unit,
    onIncomeSourceToggle: (IncomeSource) -> Unit,
    onOwnIncomeToggle: (String) -> Unit,
    onAddIncomeClick: () -> Unit,
    onAddIncome: (String) -> Unit,
    onAddIncomeDismiss: () -> Unit,
    onGoalNameChange: (String) -> Unit,
    onGoalTargetChange: (String) -> Unit,
    onGoalSavedChange: (String) -> Unit,
    onGoalDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val (title, subtitle) = stepText(uiState.step)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.step != OnboardingStep.MONEY_PLACES) {
                    AuthBackButton(onClick = onBackClick, contentDescription = "Previous step")
                } else {
                    Spacer(Modifier.size(44.dp))
                }
                Text(
                    text = "Step ${uiState.stepNumber} of ${uiState.stepCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.outline
                )
                TextButton(onClick = onSkipClick, enabled = !uiState.isSaving) {
                    Text("Skip", style = MaterialTheme.typography.labelLarge, fontSize = 14.sp)
                }
            }
            StepProgress(current = uiState.stepNumber, total = uiState.stepCount)
        }

        Column(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontSize = 26.sp, lineHeight = 32.sp)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (uiState.step) {
                OnboardingStep.MONEY_PLACES -> MoneyPlacesStep(uiState, onAccountToggle, onAddPlaceClick)
                OnboardingStep.SALARY -> SalaryStep(uiState, onPayDayClick, onBaseSalaryChange)
                OnboardingStep.INCOME_SOURCES ->
                    IncomeSourcesStep(uiState, onIncomeSourceToggle, onOwnIncomeToggle, onAddIncomeClick)
                OnboardingStep.GOAL -> GoalStep(uiState, onGoalNameChange, onGoalTargetChange, onGoalSavedChange, onGoalDateChange)
            }
        }

        Column(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (uiState.errorMessage != null) {
                AuthErrorText(uiState.errorMessage)
            }
            AuthPrimaryButton(
                text = if (uiState.isLastStep) "Finish setup" else "Continue",
                onClick = onContinueClick,
                isLoading = uiState.isSaving
            )
        }
    }

    if (uiState.isAddingPlace) {
        AddPlaceDialog(errorMessage = uiState.placeError, onAdd = onAddPlace, onDismiss = onAddPlaceDismiss)
    }
    if (uiState.isAddingIncome) {
        AddCategoryDialog(
            fixedType = EntryType.INCOME,
            errorMessage = uiState.incomeError,
            isSaving = false,
            onAdd = { name, _, _ -> onAddIncome(name) },
            onDismiss = onAddIncomeDismiss
        )
    }
}

private fun stepText(step: OnboardingStep): Pair<String, String> = when (step) {
    OnboardingStep.MONEY_PLACES -> "Where does your money sit?" to
        "Pick every place you keep money. Moving cash between them never counts as spending."
    OnboardingStep.SALARY -> "When does your salary arrive?" to
        "MoneyTrail will remind you on pay day to confirm what actually came in."
    OnboardingStep.INCOME_SOURCES -> "Where else does money come from?" to
        "Pick everything that pays you, even if it's irregular."
    OnboardingStep.GOAL -> "What are you saving for?" to
        "Set one goal. MoneyTrail shows how much to put aside each month to reach it on time."
}

@Composable
private fun ColumnScope.MoneyPlacesStep(
    uiState: OnboardingState,
    onAccountToggle: (String) -> Unit,
    onAddPlaceClick: () -> Unit
) {
    uiState.accountChoices.forEach { account ->
        ChoiceCard(
            // Savings shows "G" for goal, as in the design, so it doesn't clash with Secondary bank.
            letter = if (account.kind == AccountKind.SAVINGS) "G" else account.name.take(1),
            title = account.name,
            subtitle = account.description,
            selected = account.id in uiState.selectedAccountIds,
            onClick = { onAccountToggle(account.id) },
            enabled = !uiState.isSaving
        )
    }
    AddMoreButton(text = "Add another place", onClick = onAddPlaceClick, enabled = !uiState.isSaving)
}

@Composable
private fun ColumnScope.SalaryStep(
    uiState: OnboardingState,
    onPayDayClick: (Int) -> Unit,
    onBaseSalaryChange: (String) -> Unit
) {
    Text("Pay day", style = MaterialTheme.typography.labelMedium)
    PayDayGrid(selectedDay = uiState.payDay, onDayClick = onPayDayClick)
    Spacer(Modifier.height(8.dp))
    AuthTextField(
        value = uiState.baseSalaryDigits,
        onValueChange = onBaseSalaryChange,
        label = "Base salary (LKR)",
        placeholder = "e.g. 120000",
        enabled = !uiState.isSaving,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
    )
    AmountHint(
        text = uiState.baseSalary?.let { "LKR ${formatAmount(it)} a month, before bonuses." }
            ?: "Before bonuses. Enter 0 if you don't get a salary."
    )
}

@Composable
private fun ColumnScope.IncomeSourcesStep(
    uiState: OnboardingState,
    onIncomeSourceToggle: (IncomeSource) -> Unit,
    onOwnIncomeToggle: (String) -> Unit,
    onAddIncomeClick: () -> Unit
) {
    IncomeSource.entries.forEach { source ->
        val (title, subtitle) = incomeSourceText(source)
        ChoiceCard(
            letter = title.take(1),
            title = title,
            subtitle = subtitle,
            selected = source in uiState.incomeSources,
            onClick = { onIncomeSourceToggle(source) },
            enabled = !uiState.isSaving
        )
    }
    uiState.ownIncome.forEach { income ->
        ChoiceCard(
            letter = income.name.take(1).uppercase(),
            title = income.name,
            subtitle = "Added by you, also offered on new entries",
            selected = income.name in uiState.selectedOwnIncome,
            onClick = { onOwnIncomeToggle(income.name) },
            enabled = !uiState.isSaving
        )
    }
    AddMoreButton(text = "Add another way you earn", onClick = onAddIncomeClick, enabled = !uiState.isSaving)
}

private fun incomeSourceText(source: IncomeSource): Pair<String, String> = when (source) {
    IncomeSource.SALARY -> "Salary" to "Monthly, changes with bonuses"
    IncomeSource.FREELANCE -> "Freelance projects" to "Paid by milestone"
    IncomeSource.ADSENSE -> "Google AdSense" to "Paid monthly in US dollars"
    IncomeSource.CRYPTO -> "Crypto trading" to "Gains and losses, kept out of your savings plan"
    IncomeSource.OTHER -> "Something else" to "Gifts, refunds, anything else"
}

@Composable
private fun ColumnScope.GoalStep(
    uiState: OnboardingState,
    onGoalNameChange: (String) -> Unit,
    onGoalTargetChange: (String) -> Unit,
    onGoalSavedChange: (String) -> Unit,
    onGoalDateChange: (LocalDate) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var isDatePickerOpen by rememberSaveable { mutableStateOf(false) }
    val enabled = !uiState.isSaving

    AuthTextField(
        value = uiState.goalName,
        onValueChange = onGoalNameChange,
        label = "Goal",
        placeholder = "e.g. MacBook Pro M4",
        enabled = enabled,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next)
    )
    AuthTextField(
        value = uiState.goalTargetDigits,
        onValueChange = onGoalTargetChange,
        label = "Cost (LKR)",
        placeholder = "e.g. 490000",
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
    )
    uiState.goalTarget?.let { AmountHint("LKR ${formatAmount(it)}") }
    AuthTextField(
        value = uiState.goalSavedDigits,
        onValueChange = onGoalSavedChange,
        label = "Already saved (LKR, optional)",
        placeholder = "0",
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
    )

    Text("Reach it by", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
    OutlinedButton(
        onClick = { isDatePickerOpen = true },
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, colors.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = colors.surfaceContainerLow, contentColor = colors.onSurface),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Icon(MoneyTrailIcons.Calendar, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
        Text(
            text = uiState.goalDate.format(GoalDateFormatter),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        )
    }

    uiState.monthlySaving?.let { monthly ->
        Text(
            text = buildAnnotatedString {
                append("Put aside ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("LKR ${formatAmount(monthly)} a month") }
                append(" to reach it by ${uiState.goalDate.format(GoalMonthFormatter)}.")
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onPrimaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .background(colors.primaryContainer, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        )
    }

    if (isDatePickerOpen) {
        MoneyTrailDatePickerDialog(
            date = uiState.goalDate,
            onDateSelected = onGoalDateChange,
            onDismiss = { isDatePickerOpen = false }
        )
    }
}

@Composable
private fun AmountHint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
}

@Preview(showBackground = true, heightDp = 844)
@Composable
private fun OnboardingMoneyPlacesPreview() {
    MoneyTrailTheme {
        OnboardingPreviewContent(OnboardingState(today = LocalDate.of(2026, 10, 19)))
    }
}

@Preview(showBackground = true, heightDp = 844)
@Composable
private fun OnboardingGoalPreview() {
    MoneyTrailTheme {
        OnboardingPreviewContent(
            OnboardingState(
                today = LocalDate.of(2026, 10, 19),
                step = OnboardingStep.GOAL,
                goalName = "MacBook Pro M4",
                goalTargetDigits = "490000",
                goalSavedDigits = "11200"
            )
        )
    }
}

@Composable
private fun OnboardingPreviewContent(state: OnboardingState) {
    OnboardingContent(
        uiState = state,
        onBackClick = {},
        onSkipClick = {},
        onContinueClick = {},
        onAccountToggle = {},
        onAddPlaceClick = {},
        onAddPlace = { _, _ -> },
        onAddPlaceDismiss = {},
        onPayDayClick = {},
        onBaseSalaryChange = {},
        onIncomeSourceToggle = {},
        onOwnIncomeToggle = {},
        onAddIncomeClick = {},
        onAddIncome = {},
        onAddIncomeDismiss = {},
        onGoalNameChange = {},
        onGoalTargetChange = {},
        onGoalSavedChange = {},
        onGoalDateChange = {}
    )
}
