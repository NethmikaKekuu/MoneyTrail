package lk.moneytrail.app.presentation.freelance

import lk.moneytrail.app.domain.model.Account
import lk.moneytrail.app.domain.model.Milestone
import lk.moneytrail.app.domain.model.Project
import lk.moneytrail.app.presentation.components.formatAmount
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class UnpaidMilestoneOption(
    val optionId: String,
    val milestone: Milestone?,
    val project: Project?,
    val title: String,
    val amount: Long,
    val subtext: String
)

data class RecordFreelancePaymentUiState(
    val amountDigits: String = "30000",
    val selectedOptionId: String? = null,
    val selectedAccount: String = "Main bank",
    val selectedDate: LocalDate = LocalDate.now(),
    val accounts: List<Account> = emptyList(),
    val milestoneOptions: List<UnpaidMilestoneOption> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null
) {
    val amountValue: Long
        get() = amountDigits.toLongOrNull() ?: 0L

    val selectedOption: UnpaidMilestoneOption?
        get() = milestoneOptions.find { it.optionId == selectedOptionId } ?: milestoneOptions.firstOrNull()

    val formattedDateText: String
        get() = selectedDate.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH))

    val infoBannerMessage: String
        get() {
            val option = selectedOption
            return if (option != null && option.milestone != null) {
                "${option.milestone.title} will be marked Paid and outstanding drops by ${formatAmount(amountValue.coerceAtLeast(option.amount))}."
            } else {
                "Saved as unlinked freelance income in $selectedAccount."
            }
        }
}
