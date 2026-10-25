package lk.moneytrail.app.presentation.salary

data class BonusHistoryItem(
    val monthLabel: String,
    val amount: Long,
    val isCurrentMonth: Boolean = false
)

// Every value comes from the user's plan and entries; nothing about a real person is
// filled in here.
data class ConfirmSalaryUiState(
    val paydayText: String = "",
    val accountName: String = "",
    /** The usual pay before bonuses, from Settings. 0 when no salary is set up. */
    val baseSalary: Long = 0L,
    /** What arrived minus the base: negative when less than the base arrived. */
    val bonusAmount: Long = 0L,
    val amountCreditedDigits: String = "",
    /** False when there's no salary in Settings and none on the calendar to confirm. */
    val hasSalary: Boolean = true,
    val bonusHistory: List<BonusHistoryItem> = emptyList(),
    val averageBonus: Long = 0L,
    val highestBonus: Long = 0L,
    val highestBonusMonth: String = "",
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
) {
    val totalCredited: Long
        get() = amountCreditedDigits.toLongOrNull() ?: 0L

    val hasBonusHistory: Boolean
        get() = bonusHistory.any { it.amount > 0 }
}
