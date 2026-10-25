package lk.moneytrail.app.presentation.salary

data class BonusHistoryItem(
    val monthLabel: String,
    val amount: Long,
    val isCurrentMonth: Boolean = false
)

data class ConfirmSalaryUiState(
    val paydayText: String = "Payday · Sun 25 Oct",
    val accountName: String = "Main bank",
    val baseSalary: Long = 120_000L,
    val bonusAmount: Long = 0L,
    val amountCreditedDigits: String = "120000",
    val bonusHistory: List<BonusHistoryItem> = emptyList(),
    val averageBonus: Long = 0L,
    val highestBonus: Long = 0L,
    val highestBonusMonth: String = "",
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
) {
    val totalCredited: Long
        get() = amountCreditedDigits.toLongOrNull() ?: (baseSalary + bonusAmount)

    val hasBonusHistory: Boolean
        get() = bonusHistory.any { it.amount > 0 }
}
