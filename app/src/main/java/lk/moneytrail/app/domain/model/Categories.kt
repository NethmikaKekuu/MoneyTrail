package lk.moneytrail.app.domain.model

/**
 * The categories offered on the new entry screen. Each expense category says whether
 * it's usually committed or discretionary, so the app can show how much of Kavindu's
 * spending was a choice without asking him every time.
 */
object Categories {

    val expense: Map<String, SpendingType> = linkedMapOf(
        "Food out" to SpendingType.DISCRETIONARY,
        "Coffee" to SpendingType.DISCRETIONARY,
        "Rides" to SpendingType.DISCRETIONARY,
        "Groceries" to SpendingType.COMMITTED,
        "Food delivery" to SpendingType.DISCRETIONARY,
        "Utilities" to SpendingType.COMMITTED,
        "Rent" to SpendingType.COMMITTED,
        "Other" to SpendingType.DISCRETIONARY
    )

    val income: List<String> = listOf("Salary", "Freelance", "AdSense", "Crypto", "Other")

    fun namesFor(type: EntryType): List<String> = when (type) {
        EntryType.EXPENSE -> expense.keys.toList()
        EntryType.INCOME -> income
    }
}

/**
 * Accounts offered on the new entry screen for someone who skipped onboarding.
 * Once onboarding is finished, the accounts they picked are used instead.
 */
object Accounts {
    val defaults: List<String> = listOf("Main bank", "Secondary bank", "Cash wallet", "Savings")
}
