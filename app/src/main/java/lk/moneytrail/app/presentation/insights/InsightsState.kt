package lk.moneytrail.app.presentation.insights

import lk.moneytrail.app.domain.model.cryptoTotal
import lk.moneytrail.app.domain.model.dependability
import lk.moneytrail.app.domain.model.IncomeDependability
import lk.moneytrail.app.domain.model.lastMonthsBySource
import lk.moneytrail.app.domain.model.MonthBySource
import lk.moneytrail.app.domain.model.monthIncome
import lk.moneytrail.app.domain.model.MonthIncome
import lk.moneytrail.app.domain.model.CategoryTotal
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.MonthTotal
import lk.moneytrail.app.domain.model.SavingsGoal
import lk.moneytrail.app.domain.model.SpendingSplit
import lk.moneytrail.app.domain.model.categoryTotals
import lk.moneytrail.app.domain.model.daysOfSaving
import lk.moneytrail.app.domain.model.diningSpend
import lk.moneytrail.app.domain.model.lastMonthsTotals
import lk.moneytrail.app.domain.model.spendingSplit
import java.time.LocalDate
import java.time.YearMonth

enum class InsightsTab { EXPENSES, INCOME }

data class InsightsState(
    val today: LocalDate,
    val tab: InsightsTab = InsightsTab.EXPENSES,
    /** True until the first entries arrive. */
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    /** Every entry over the months in the chart, ending with the current month. */
    val entries: List<Entry> = emptyList(),
    /** The goal from onboarding; null for someone who skipped it. */
    val goal: SavingsGoal? = null
) {
    val month: YearMonth
        get() = YearMonth.from(today)

    /** This month's spending so far. */
    val spent: MonthTotal
        get() = lastMonths.last()

    val split: SpendingSplit
        get() = entries.spendingSplit(month)

    val categories: List<CategoryTotal>
        get() = entries.categoryTotals(EntryType.EXPENSE, month)

    /** One bar per month for the chart, oldest first, this month last. */
    val lastMonths: List<MonthTotal>
        get() = entries.lastMonthsTotals(EntryType.EXPENSE, month, CHART_MONTHS)

    /** This month's coffee and dining out as days of goal saving; null without a goal or spending. */
    val diningDaysOfSaving: Int?
        get() = goal?.daysOfSaving(entries.diningSpend(month), today)

    /** This month's income: arrived so far, and still expected. */
    val income: MonthIncome
        get() = entries.monthIncome(month)

    /** One bar per month for the income chart, oldest first, this month last. */
    val incomeBySource: List<MonthBySource>
        get() = entries.lastMonthsBySource(month, CHART_MONTHS)

    /** Last month, the latest one that is complete, for how dependable income is. */
    val lastFullMonth: YearMonth
        get() = month.minusMonths(1)

    val dependability: IncomeDependability
        get() = entries.dependability(lastFullMonth)

    val cryptoThisYear: Long
        get() = entries.cryptoTotal(today.year)

    companion object {
        const val CHART_MONTHS = 6
    }
}
