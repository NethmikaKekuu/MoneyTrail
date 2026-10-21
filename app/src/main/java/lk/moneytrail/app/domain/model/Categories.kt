package lk.moneytrail.app.domain.model

/**
 * The built-in categories offered on the new entry screen; users can add their own
 * ([CustomCategory]). Each expense category says whether it's usually committed or
 * discretionary, so the app can show how much of Kavindu's spending was a choice
 * without asking him every time.
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

    /** A salary's bonus is kept on the salary entry itself ([Entry.bonus]), not as a category. */
    const val SALARY = "Salary"

    val income: List<String> = listOf(SALARY, "Freelance", "AdSense", "Crypto", "Other")

    fun namesFor(type: EntryType): List<String> = when (type) {
        EntryType.EXPENSE -> expense.keys.toList()
        EntryType.INCOME -> income
        EntryType.TRANSFER -> emptyList()
    }

    /** The built-in names for [type] followed by the user's own, without repeats. */
    fun namesFor(type: EntryType, custom: List<CustomCategory>): List<String> =
        (namesFor(type) + custom.filter { it.type == type }.map { it.name }).distinct()

    /** The usual committed or discretionary setting of an expense category, built-in or the user's own. */
    fun defaultSpending(category: String, custom: List<CustomCategory>): SpendingType? =
        expense[category] ?: custom.find { it.type == EntryType.EXPENSE && it.name == category }?.spendingType
}

/**
 * Accounts offered on the new entry screen for someone who skipped onboarding.
 * Once onboarding is finished, the accounts they picked are used instead.
 */
object Accounts {
    val defaults: List<String> = listOf("Main bank", "Secondary bank", "Cash wallet", "Savings")
}
