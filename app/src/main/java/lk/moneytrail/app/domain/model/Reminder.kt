package lk.moneytrail.app.domain.model

/**
 * Something the daily reminder tells the user as a system notification, so it reaches
 * Kavindu even on days he doesn't open the app. Each one matches a card inside the app.
 */
sealed interface Reminder {

    /** Planned money whose day has come and isn't checked yet, oldest first: "Did it come in?". */
    data class MoneyDue(val entries: List<Entry>) : Reminder

    /** Pay yourself first: LKR [amount] to move to savings this month. */
    data class PayYourselfFirst(val amount: Long) : Reminder

    /** Back after a break: [daysAway] days without logging anything. */
    data class CatchUp(val daysAway: Long) : Reminder
}
