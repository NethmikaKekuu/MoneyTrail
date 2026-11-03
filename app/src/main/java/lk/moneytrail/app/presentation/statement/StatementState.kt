package lk.moneytrail.app.presentation.statement

import lk.moneytrail.app.domain.model.Account
import lk.moneytrail.app.domain.model.AccountStatement
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.MonthClose
import lk.moneytrail.app.domain.model.StatementTotals
import lk.moneytrail.app.domain.model.accountStatements
import lk.moneytrail.app.domain.model.statementTotals
import lk.moneytrail.app.domain.model.toStatementCsv
import java.time.LocalDate
import java.time.YearMonth

data class StatementState(
    val month: YearMonth,
    /** The latest month that can be picked: this one. */
    val latestMonth: YearMonth,
    /** Today, for the "Exported on" line of the CSV. */
    val today: LocalDate = latestMonth.atDay(1),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val accounts: List<Account> = emptyList(),
    /** Entries from the first balance check up to the end of [month], for the opening balances. */
    val entries: List<Entry> = emptyList(),
    val close: MonthClose? = null,
    val previousClose: MonthClose? = null,
    val isExporting: Boolean = false,
    /** A one-off message after an export, until it has been shown. */
    val message: String? = null
) {
    val totals: StatementTotals
        get() = entries.statementTotals(month)

    val lines: List<AccountStatement>
        get() = accountStatements(month, accounts, entries, previousClose)

    /** True when the month has nothing confirmed to show. */
    val isEmpty: Boolean
        get() = entries.none { YearMonth.from(it.date) == month && !it.isExpected }

    val canGoForward: Boolean
        get() = month.isBefore(latestMonth)

    val csv: String
        get() = entries.toStatementCsv(month, today)

    /** File name offered in the save picker, e.g. moneytrail-2026-09.csv. */
    val fileName: String
        get() = "moneytrail-$month.csv"
}
