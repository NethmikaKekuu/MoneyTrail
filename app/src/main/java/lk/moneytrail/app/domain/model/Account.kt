package lk.moneytrail.app.domain.model

import java.time.LocalDate

enum class AccountKind { BANK, CASH, SAVINGS, CRYPTO }

/** A place the user keeps money: the users/{uid}/accounts/{id} document in Firestore. */
data class Account(
    /** A short slug such as "main-bank", also the Firestore document id. */
    val id: String,
    val name: String,
    val description: String,
    val kind: AccountKind,
    /** The real balance the user last typed in when balancing it (catch-up), in LKR; null before the first time. */
    val checkedBalance: Long? = null,
    /** The day [checkedBalance] was typed in. */
    val checkedOn: LocalDate? = null
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

    /**
     * A place the user named in onboarding, such as "Sampath Bank". Its id is the name as
     * a slug ("sampath-bank"), with a number added when [takenIds] already has it.
     */
    fun custom(name: String, kind: AccountKind, takenIds: Set<String>): Account {
        val base = name.trim().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(40).ifEmpty { "place" }
        var id = base
        var number = 2
        while (id in takenIds) id = "$base-${number++}"

        val description = when (kind) {
            AccountKind.BANK -> "Bank account you added"
            AccountKind.CASH -> "Cash you added"
            AccountKind.SAVINGS -> "Savings you added"
            AccountKind.CRYPTO -> "Crypto you added"
        }
        return Account(id, name.trim(), description, kind)
    }
}
