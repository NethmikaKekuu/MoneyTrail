package lk.moneytrail.app.presentation.freelance

import lk.moneytrail.app.domain.model.MilestoneStatus
import java.time.temporal.ChronoUnit
import java.time.ZoneId
import java.time.Instant
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
    val amountDigits: String = "",
    val selectedOptionId: String? = null,
    val selectedAccount: String = "",
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

/** The milestone a payment most likely is for: the oldest invoiced one, else the next planned one. */
fun Project.nextUnpaidMilestone(): Milestone? =
    milestones.filter { it.status == MilestoneStatus.INVOICED }.minByOrNull { it.dueTimestamp }
        ?: milestones.filter { it.status == MilestoneStatus.PLANNED }.minByOrNull { it.dueTimestamp }

/** "invoiced, due 14 Oct", "planned, overdue 3 days" and so on, from the milestone's real due date. */
fun Milestone.dueText(today: LocalDate): String {
    val label = if (status == MilestoneStatus.INVOICED) "invoiced" else "planned"
    if (dueTimestamp <= 0) return label
    val due = Instant.ofEpochMilli(dueTimestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    val late = ChronoUnit.DAYS.between(due, today)
    return when {
        late > 1 -> "$label, overdue $late days"
        late == 1L -> "$label, overdue 1 day"
        late == 0L -> "$label, due today"
        else -> "$label, due ${due.format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))}"
    }
}
