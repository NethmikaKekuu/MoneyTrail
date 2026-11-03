package lk.moneytrail.app.domain.model

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

// Monthly close (MoneyTrail-Figma-HTML/21-MonthlyClose.html) and the statement
// (22-Statement.html): once a month every account is checked against the bank, so the
// month's numbers can be trusted, and the month can be exported for a spreadsheet.

/** A closed month: the real balance of each account at the end of it, by account id. */
data class MonthClose(
    val month: YearMonth,
    val balances: Map<String, Long>,
    val closedOn: LocalDate
)

/**
 * What [this] account held at the end of [day], from its last balance check and the
 * entries logged since. Null when it was first checked after that day, so there is
 * nothing to work it out from.
 */
fun Account.balanceAtEndOf(day: LocalDate, entries: List<Entry>): Long? {
    val checkedOn = checkedOn ?: return null
    val start = checkedBalance ?: return null
    if (checkedOn.isAfter(day.plusDays(1))) return null
    // The check is the balance at the start of checkedOn, so that day's entries count.
    return start + entries.filter { !it.date.isBefore(checkedOn) && !it.date.isAfter(day) }.balanceChange(name)
}

/** The month's totals at the top of the statement. "Not logged" money is shown on its own. */
data class StatementTotals(
    val income: Long,
    val expenses: Long,
    /** Money that left without being logged, found when balancing (less any that came in unlogged). */
    val unaccounted: Long
) {
    /** What the month left over. */
    val kept: Long
        get() = income - expenses - unaccounted
}

private fun List<Entry>.confirmedIn(month: YearMonth): List<Entry> =
    filter { !it.isExpected && YearMonth.from(it.date) == month }

fun List<Entry>.statementTotals(month: YearMonth): StatementTotals {
    val entries = confirmedIn(month)
    val logged = entries.filter { it.category != Categories.NOT_LOGGED }
    val notLogged = entries.filter { it.category == Categories.NOT_LOGGED }
    return StatementTotals(
        income = logged.filter { it.type == EntryType.INCOME }.sumOf { it.amount },
        expenses = logged.filter { it.type == EntryType.EXPENSE }.sumOf { it.amount },
        unaccounted = notLogged.sumOf { if (it.type == EntryType.EXPENSE) it.amount else -it.amount }
    )
}

/** One account's line on the statement: opening + in − out = closing. */
data class AccountStatement(
    val account: Account,
    /** Null when the account was first checked after the month began. */
    val opening: Long?,
    val moneyIn: Long,
    val moneyOut: Long
) {
    val closing: Long?
        get() = opening?.let { it + moneyIn - moneyOut }
}

/**
 * Each account's line for [month]. The opening balance is last month's close when there
 * is one, else it's worked out from the account's balance check.
 */
fun accountStatements(
    month: YearMonth,
    accounts: List<Account>,
    entries: List<Entry>,
    previousClose: MonthClose?
): List<AccountStatement> {
    val inMonth = entries.confirmedIn(month)
    val dayBefore = month.atDay(1).minusDays(1)
    return accounts.map { account ->
        val own = inMonth.filter { it.account == account.name }
        AccountStatement(
            account = account,
            opening = previousClose?.balances?.get(account.id) ?: account.balanceAtEndOf(dayBefore, entries),
            moneyIn = own.filter { it.type == EntryType.INCOME }.sumOf { it.amount },
            moneyOut = own.filter { it.type == EntryType.EXPENSE }.sumOf { it.amount }
        )
    }
}

private val CsvDate: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

/** A CSV cell: quoted when it holds a comma, a quote or a line break. */
private fun csvCell(value: String): String =
    if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\"" else value

/**
 * The month's confirmed entries as CSV, oldest first, for a spreadsheet. Amounts are in
 * LKR, what really arrived or left; an AdSense payout also says its dollars in the note.
 * Expenses are negative, so a spreadsheet SUM of the amount column gives the month's net.
 */
fun List<Entry>.toStatementCsv(month: YearMonth): String {
    val rows = confirmedIn(month).sortedWith(compareBy({ it.date }, { it.type }, { it.category }))
    return buildString {
        append("date,type,category,account,amount,currency,note\r\n")
        rows.forEach { entry ->
            val signed = if (entry.type == EntryType.EXPENSE) -entry.amount else entry.amount
            val note = if (entry.isUsd) {
                val dollars = String.format(Locale.US, "USD %d.%02d", entry.usdCents / 100, entry.usdCents % 100)
                listOf(entry.note, dollars).filter { it.isNotBlank() }.joinToString(" · ")
            } else {
                entry.note
            }
            append(
                listOf(
                    entry.date.format(CsvDate),
                    entry.type.name.lowercase(Locale.ENGLISH),
                    entry.category,
                    entry.account,
                    signed.toString(),
                    "LKR",
                    note
                ).joinToString(",") { csvCell(it) }
            )
            append("\r\n")
        }
    }
}
