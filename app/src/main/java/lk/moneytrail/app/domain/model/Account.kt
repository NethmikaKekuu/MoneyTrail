package lk.moneytrail.app.domain.model

enum class AccountKind { BANK, CASH, SAVINGS, CRYPTO }

/** A place the user keeps money: the users/{uid}/accounts/{id} document in Firestore. */
data class Account(
    /** A short slug such as "main-bank", also the Firestore document id. */
    val id: String,
    val name: String,
    val description: String,
    val kind: AccountKind
)

/** The money places offered in onboarding step 1, as in MoneyTrail-Figma-HTML/06-Onboarding.html. */
object AccountTemplates {

    val all: List<Account> = listOf(
        Account("main-bank", "Main bank", "Card and salary account", AccountKind.BANK),
        Account("secondary-bank", "Secondary bank", "Gym and subscriptions", AccountKind.BANK),
        Account("cash-wallet", "Cash wallet", "ATM withdrawals and small buys", AccountKind.CASH),
        Account("savings", "Savings", "For your goal", AccountKind.SAVINGS),
        Account("binance", "Binance", "Crypto exchange, CSV import later", AccountKind.CRYPTO)
    )

    /** Ticked to begin with, as in the design. */
    val defaultIds: Set<String> = setOf("main-bank", "secondary-bank", "cash-wallet", "savings")
}
