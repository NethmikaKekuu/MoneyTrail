package lk.moneytrail.app.presentation.salary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.moneytrail.app.domain.model.Accounts
import lk.moneytrail.app.domain.model.Categories
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryStatus
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.confirmedSalaryFromTotal
import lk.moneytrail.app.domain.repository.EntryRepository
import lk.moneytrail.app.domain.repository.PlanRepository
import lk.moneytrail.app.domain.usecase.UpdateSalaryUseCase
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * Everything about the salary in one place: confirming what arrived this month, the
 * bonus history, and the pay day and base salary that onboarding set.
 */
@HiltViewModel
class ConfirmSalaryViewModel @Inject constructor(
    private val planRepository: PlanRepository,
    private val entryRepository: EntryRepository,
    private val updateSalary: UpdateSalaryUseCase
) : ViewModel() {

    private val today = LocalDate.now()
    private val currentMonth = YearMonth.from(today)
    private val _uiState = MutableStateFlow(ConfirmSalaryUiState())
    val uiState: StateFlow<ConfirmSalaryUiState> = _uiState.asStateFlow()

    private var targetEntry: Entry? = null
    private var bonusJob: Job? = null

    init {
        loadData()
    }

    /** Loads the plan and this month's salary; after saving the salary it reloads without the spinner. */
    private fun loadData(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) _uiState.update { it.copy(isLoading = true) }

            val planResult = planRepository.getPlan()
            val plan = planResult.getOrNull()
            val baseSalary = plan?.baseSalary ?: 0L

            // The salary to confirm: an expected one (this month's, or an older one never
            // checked), or else this month's if it's already confirmed, so confirming again
            // corrects it instead of adding a second salary.
            val expectedSalary = entryRepository.observeExpected().first().getOrDefault(emptyList())
                .firstOrNull { it.isSalary }
            val confirmedThisMonth = entryRepository.observeMonth(currentMonth).first().getOrDefault(emptyList())
                .lastOrNull { it.isSalary && !it.isExpected }
            val salaryEntry = expectedSalary ?: confirmedThisMonth
            targetEntry = salaryEntry

            val amount = salaryEntry?.amount ?: baseSalary

            _uiState.update {
                it.copy(
                    isLoading = false,
                    hasSalary = baseSalary > 0 || salaryEntry != null,
                    // A plan that failed to load isn't the same as a skipped setup.
                    hasPlan = planResult.isFailure || plan != null,
                    payDay = plan?.payDay ?: it.payDay,
                    monthTitle = "${(salaryEntry?.date ?: today).format(MonthTitleFormatter)} salary",
                    statusText = statusFor(salaryEntry),
                    isConfirmed = salaryEntry != null && !salaryEntry.isExpected,
                    accountName = salaryEntry?.account ?: Accounts.defaults.first(),
                    baseSalary = baseSalary,
                    bonusAmount = amount - baseSalary,
                    amountCreditedDigits = amount.takeIf { amount > 0 }?.toString().orEmpty()
                )
            }

            loadBonusHistory()
        }
    }

    private fun loadBonusHistory() {
        bonusJob?.cancel()
        bonusJob = viewModelScope.launch {
            val fromDate = today.minusMonths(5).withDayOfMonth(1)
            entryRepository.observeBetween(fromDate, today).collect { result ->
                result.onSuccess { entries ->
                    val confirmedSalaries = entries.filter {
                        it.type == EntryType.INCOME &&
                        it.category == Categories.SALARY &&
                        it.status == EntryStatus.CONFIRMED
                    }

                    buildBonusHistoryState(confirmedSalaries)
                }
            }
        }
    }

    private fun buildBonusHistoryState(confirmedSalaries: List<Entry>) {
        val formatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)
        val currentBonus = _uiState.value.bonusAmount.coerceAtLeast(0L)

        // Map confirmed salary entries by YearMonth
        val salaryByMonth = confirmedSalaries.associateBy { YearMonth.from(it.date) }

        // Determine all months to show: confirmed months in last 6 months + current month
        val monthsToShow = mutableListOf<YearMonth>()
        for (i in 5 downTo 0) {
            val month = currentMonth.minusMonths(i.toLong())
            if (month in salaryByMonth || month == currentMonth) {
                monthsToShow.add(month)
            }
        }

        val historyItems = monthsToShow.map { month ->
            val isCurrent = (month == currentMonth)
            val bonus = if (isCurrent) {
                salaryByMonth[month]?.bonus ?: currentBonus
            } else {
                salaryByMonth[month]?.bonus ?: 0L
            }
            BonusHistoryItem(
                monthLabel = month.format(formatter),
                amount = bonus,
                isCurrentMonth = isCurrent
            )
        }

        val validBonuses = historyItems.filter { it.amount > 0 }
        val avgBonus = if (validBonuses.isNotEmpty()) validBonuses.map { it.amount }.average().toLong() else 0L
        val highestItem = validBonuses.maxByOrNull { it.amount }

        _uiState.update {
            it.copy(
                bonusHistory = historyItems,
                averageBonus = avgBonus,
                highestBonus = highestItem?.amount ?: 0L,
                highestBonusMonth = highestItem?.monthLabel.orEmpty()
            )
        }
    }

    fun onAmountCreditedChange(input: String) {
        val digits = input.filter(Char::isDigit).trimStart('0').take(MAX_DIGITS)
        val newTotal = digits.toLongOrNull() ?: 0L
        val base = _uiState.value.baseSalary
        val newBonus = newTotal - base

        _uiState.update { state ->
            val updatedHistory = state.bonusHistory.map {
                if (it.isCurrentMonth) it.copy(amount = newBonus.coerceAtLeast(0L)) else it
            }
            state.copy(
                amountCreditedDigits = digits,
                bonusAmount = newBonus,
                bonusHistory = updatedHistory
            )
        }
    }

    fun onConfirmSalary(onConfirmed: () -> Unit) {
        val state = _uiState.value
        if (state.isSubmitting) return

        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

        viewModelScope.launch {
            val total = state.totalCredited

            val entryToSave = targetEntry ?: Entry(
                id = "",
                type = EntryType.INCOME,
                amount = total,
                category = Categories.SALARY,
                account = state.accountName,
                date = today,
                note = "",
                spendingType = null
            )

            // Saves exactly what arrived; anything above the base is this month's bonus.
            val confirmedEntry = entryToSave.confirmedSalaryFromTotal(total = total, base = state.baseSalary, date = today)
            val bonus = confirmedEntry.bonus

            val result = if (entryToSave.id.isEmpty()) {
                entryRepository.addEntry(confirmedEntry)
            } else {
                entryRepository.updateEntry(confirmedEntry)
            }

            result.onSuccess {
                _uiState.update { currentState ->
                    val formatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)
                    val currentMonthLabel = currentMonth.format(formatter)

                    val updatedHistory = if (currentState.bonusHistory.any { it.isCurrentMonth }) {
                        currentState.bonusHistory.map {
                            if (it.isCurrentMonth) it.copy(amount = bonus) else it
                        }
                    } else {
                        currentState.bonusHistory + BonusHistoryItem(currentMonthLabel, bonus, isCurrentMonth = true)
                    }

                    currentState.copy(
                        isSubmitting = false,
                        bonusHistory = updatedHistory
                    )
                }
                onConfirmed()
            }.onFailure { error ->
                _uiState.update { it.copy(isSubmitting = false, errorMessage = error.message ?: "Failed to confirm salary") }
            }
        }
    }

    /** "Due 25 Oct", "Due today", "Overdue", "Not confirmed yet" or "Confirmed". */
    private fun statusFor(salary: Entry?): String = when {
        salary == null -> "Not confirmed yet"
        !salary.isExpected -> "Confirmed"
        salary.date == today -> "Due today"
        salary.date.isBefore(today) -> "Overdue"
        else -> "Due ${salary.date.format(DayFormatter)}"
    }

    /** Opens "Pay day and base salary" with what's saved now. */
    fun onEditSalaryClick() {
        _uiState.update {
            it.copy(
                isEditingSalary = true,
                payDayDigits = it.payDay?.toString().orEmpty(),
                baseSalaryDigits = it.baseSalary.takeIf { base -> base > 0 }?.toString().orEmpty(),
                salarySettingsError = null
            )
        }
    }

    fun onEditSalaryDismiss() {
        _uiState.update { it.copy(isEditingSalary = false, salarySettingsError = null) }
    }

    fun onPayDayChange(text: String) {
        _uiState.update { it.copy(payDayDigits = text.filter(Char::isDigit).take(2), salarySettingsError = null) }
    }

    fun onBaseSalaryChange(text: String) {
        _uiState.update { it.copy(baseSalaryDigits = text.filter(Char::isDigit).take(MAX_DIGITS), salarySettingsError = null) }
    }

    /** Saves the pay day and base salary; this month's expected salary follows (UpdateSalaryUseCase). */
    fun onSaveSalaryClick() {
        val state = _uiState.value
        if (state.isSavingSalary) return

        _uiState.update { it.copy(isSavingSalary = true, salarySettingsError = null) }
        viewModelScope.launch {
            updateSalary(state.payDayDigits.toIntOrNull(), state.baseSalaryDigits.toLongOrNull(), today)
                .onSuccess {
                    _uiState.update { it.copy(isSavingSalary = false, isEditingSalary = false) }
                    loadData(showLoading = false)
                }
                .onFailure { e -> _uiState.update { it.copy(isSavingSalary = false, salarySettingsError = e.message) } }
        }
    }

    private companion object {
        /** LKR 999,999,999 at most, as everywhere else in the app. */
        const val MAX_DIGITS = 9
        val MonthTitleFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)
        val DayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    }
}
