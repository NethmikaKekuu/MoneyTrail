package lk.moneytrail.app.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.moneytrail.app.domain.model.MoneyPlan
import lk.moneytrail.app.domain.model.confirmed
import lk.moneytrail.app.domain.model.confirmedSalary
import lk.moneytrail.app.domain.model.dueExpected
import lk.moneytrail.app.domain.model.isSavingsDue
import lk.moneytrail.app.domain.model.monthlySavings
import lk.moneytrail.app.domain.repository.AuthRepository
import lk.moneytrail.app.domain.repository.EntryRepository
import lk.moneytrail.app.domain.repository.PlanRepository
import lk.moneytrail.app.domain.repository.UserRepository
import lk.moneytrail.app.domain.usecase.EnsureMonthlySalaryUseCase
import lk.moneytrail.app.domain.usecase.UpdateEntryUseCase
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val entryRepository: EntryRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val planRepository: PlanRepository,
    private val updateEntryUseCase: UpdateEntryUseCase,
    private val ensureMonthlySalary: EnsureMonthlySalaryUseCase
) : ViewModel() {

    private val today = LocalDate.now()

    private val _uiState = MutableStateFlow(
        CalendarState(month = YearMonth.from(today), today = today, selectedDate = today)
    )
    val uiState: StateFlow<CalendarState> = _uiState.asStateFlow()

    /** The live listener for the month on screen. Replaced when the month changes. */
    private var monthJob: Job? = null

    /** The latest plan, to know if confirming the salary should lead to Pay yourself first. */
    private var plan: MoneyPlan? = null

    init {
        loadFirstName()
        listenToMonth(_uiState.value.month)
        listenToExpected()
        listenToSavings()
        viewModelScope.launch {
            // Puts this month's salary on the calendar as expected. If it fails, the
            // user can still add it by hand, so there's nothing to show.
            ensureMonthlySalary(today)
        }
    }

    fun onPreviousMonthClick() {
        showMonth(_uiState.value.month.minusMonths(1))
    }

    fun onNextMonthClick() {
        showMonth(_uiState.value.month.plusMonths(1))
    }

    fun onDayClick(date: LocalDate) {
        if (YearMonth.from(date) == _uiState.value.month) {
            _uiState.update { it.copy(selectedDate = date) }
        }
    }

    fun onRetryClick() {
        listenToMonth(_uiState.value.month)
    }

    private fun showMonth(month: YearMonth) {
        // Land on today when it's in that month, otherwise on the 1st.
        val selected = if (YearMonth.from(today) == month) today else month.atDay(1)
        _uiState.update { it.copy(month = month, selectedDate = selected, entries = emptyList()) }
        listenToMonth(month)
    }

    private fun listenToMonth(month: YearMonth) {
        monthJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        monthJob = viewModelScope.launch {
            entryRepository.observeMonth(month).collect { result ->
                result
                    .onSuccess { entries ->
                        _uiState.update { it.copy(isLoading = false, errorMessage = null, entries = entries) }
                    }
                    .onFailure { e ->
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = e.message ?: "Couldn't load your entries.")
                        }
                    }
            }
        }
    }

    /** Opens "Did it come in?" for the oldest expected entry that's due. A salary starts with its base pay. */
    fun onCheckDueClick() {
        val entry = _uiState.value.dueExpected.firstOrNull() ?: return
        val check = ExpectedCheck(
            entry = entry,
            amountDigits = entry.baseAmount.toString(),
            date = today,
            bonusDigits = entry.bonus.takeIf { it > 0 }?.toString().orEmpty()
        )
        _uiState.update { it.copy(check = check) }
    }

    fun onCheckAmountChange(text: String) {
        updateCheck { copy(amountDigits = text.filter(Char::isDigit).trimStart('0').take(MAX_DIGITS), errorMessage = null) }
    }

    fun onCheckBonusChange(text: String) {
        updateCheck { copy(bonusDigits = text.filter(Char::isDigit).trimStart('0').take(MAX_DIGITS), errorMessage = null) }
    }

    fun onCheckDateChange(date: LocalDate) {
        updateCheck { copy(date = date, errorMessage = null) }
    }

    /**
     * The money moved: the entry becomes confirmed with the real amount and date. A
     * salary's bonus goes on the salary itself, so it shows as one payment with the
     * base and the bonus told apart.
     */
    fun onCheckConfirm() {
        val isSalary = _uiState.value.check?.isSalary == true
        saveCheck(onSaved = { if (isSalary) openGoalIfSavingsToMove() }) { check ->
            val amount = check.amountDigits.toLongOrNull() ?: 0L
            val confirmed = if (check.isSalary) {
                check.entry.confirmedSalary(base = amount, bonus = check.bonus, date = check.date)
            } else {
                check.entry.confirmed(amount, check.date)
            }
            updateEntryUseCase(confirmed)
        }
    }

    fun onCheckNotYetClick() {
        updateCheck { copy(isPickingNewDate = true, errorMessage = null) }
    }

    fun onCheckNewDateDismiss() {
        updateCheck { copy(isPickingNewDate = false) }
    }

    /** Not yet: the entry stays expected and moves to [date]. */
    fun onCheckReschedule(date: LocalDate) {
        if (!date.isAfter(today)) {
            updateCheck { copy(isPickingNewDate = false, errorMessage = "Pick a date after today.") }
            return
        }
        saveCheck { check -> updateEntryUseCase(check.entry.copy(date = date)) }
    }

    /** It didn't happen: the expected entry is removed. */
    fun onCheckRemove() {
        saveCheck { check -> entryRepository.deleteEntry(check.entry.id) }
    }

    fun onCheckDismiss() {
        _uiState.update { it.copy(check = null) }
    }

    /** Every expected entry, so ones due from earlier months still show. */
    private fun listenToExpected() {
        viewModelScope.launch {
            entryRepository.observeExpected().collect { result ->
                // Only the "due" card depends on this, so a failure just hides it.
                result.onSuccess { expected ->
                    _uiState.update { it.copy(dueExpected = expected.dueExpected(today)) }
                }
            }
        }
    }

    private fun saveCheck(onSaved: () -> Unit = {}, action: suspend (ExpectedCheck) -> Result<Unit>) {
        val check = _uiState.value.check ?: return
        if (check.isSaving) return

        updateCheck { copy(isSaving = true, isPickingNewDate = false, errorMessage = null) }
        viewModelScope.launch {
            action(check)
                .onSuccess {
                    _uiState.update { it.copy(check = null) }
                    onSaved()
                }
                .onFailure { e -> updateCheck { copy(isSaving = false, errorMessage = e.message) } }
        }
    }

    /** The Goal tab has opened after the salary was confirmed. */
    fun onGoalOpened() {
        _uiState.update { it.copy(openGoal = false) }
    }

    /**
     * Pay yourself first: once this month's salary is confirmed or the transfer day has
     * come, the calendar shows what to move to savings, until it's moved or snoozed.
     */
    private fun listenToSavings() {
        viewModelScope.launch {
            combine(planRepository.observePlan(), entryRepository.observeMonth(YearMonth.from(today))) { planResult, entries ->
                plan = planResult.getOrNull()
                val isSalaryConfirmed = entries.getOrNull().orEmpty().any { it.isSalary && !it.isExpected }
                // Only this card depends on it, so a failure just leaves it out.
                plan?.takeIf { it.isSavingsDue(today, isSalaryConfirmed) }?.monthlySavings(today)?.values?.sum()
            }.collect { amount -> _uiState.update { it.copy(savingsDue = amount) } }
        }
    }

    /** After the salary is confirmed, goes on to Pay yourself first when there's something to save. */
    private fun openGoalIfSavingsToMove() {
        val plan = plan ?: return
        if (plan.monthlySavings(today).isNotEmpty() && plan.savingsMovedMonth != YearMonth.from(today)) {
            _uiState.update { it.copy(openGoal = true) }
        }
    }

    private fun updateCheck(change: ExpectedCheck.() -> ExpectedCheck) {
        _uiState.update { state -> state.check?.let { state.copy(check = it.change()) } ?: state }
    }

    private fun loadFirstName() {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            // Only the greeting depends on this, so a failure just leaves it as "Hi there".
            userRepository.getProfile(uid).onSuccess { profile ->
                val firstName = profile?.name?.trim()?.substringBefore(' ')?.takeIf { it.isNotEmpty() }
                _uiState.update { it.copy(firstName = firstName) }
            }
        }
    }

    private companion object {
        /** LKR 999,999,999 at most, as everywhere else in the app. */
        const val MAX_DIGITS = 9
    }
}
