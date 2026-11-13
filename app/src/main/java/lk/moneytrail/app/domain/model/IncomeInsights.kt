package lk.moneytrail.app.domain.model

import java.time.YearMonth
import kotlin.math.roundToLong

/** The name a source is shown with. */
val IncomeSource.label: String
    get() = when (this) {
        IncomeSource.SALARY -> "Salary"
        IncomeSource.FREELANCE -> "Freelance"
        IncomeSource.ADSENSE -> "AdSense"
        IncomeSource.CRYPTO -> "Crypto"
        IncomeSource.OTHER -> "Other"
    }

/**
 * Where an income entry came from ([IncomeSource] is also what onboarding asks about).
 * Kavindu's channels each behave differently, so the insights show them apart.
 */
val Entry.incomeSource: IncomeSource
    get() = when {
        isSalary -> IncomeSource.SALARY
        category == Categories.ADSENSE || isUsd -> IncomeSource.ADSENSE
        category == "Freelance" -> IncomeSource.FREELANCE
        category == "Crypto" -> IncomeSource.CRYPTO
        else -> IncomeSource.OTHER
    }

/** Confirmed income only: what really arrived, not what was planned. */
private fun List<Entry>.confirmedIncome(month: YearMonth): List<Entry> =
    filter { it.type == EntryType.INCOME && !it.isExpected && YearMonth.from(it.date) == month }

/** Income in [month]: what has arrived so far, and what is still planned but not confirmed. */
data class MonthIncome(val confirmed: Long, val expected: Long)

fun List<Entry>.monthIncome(month: YearMonth): MonthIncome {
    val income = filter { it.type == EntryType.INCOME && YearMonth.from(it.date) == month }
    return MonthIncome(
        confirmed = income.filter { !it.isExpected }.sumOf { it.amount },
        expected = income.filter { it.isExpected }.sumOf { it.amount }
    )
}

/** One month's confirmed income split by source, crypto left out (see [cryptoTotal]). */
data class MonthBySource(val month: YearMonth, val bySource: Map<IncomeSource, Long>) {
    val total: Long
        get() = bySource.values.sum()

    fun amount(source: IncomeSource): Long = bySource[source] ?: 0
}

/**
 * The [count] months up to and including [lastMonth], oldest first, each split by
 * source. Months with nothing are kept as zero so the chart always has every bar.
 * Crypto is left out: it can swing either way, so it isn't income to plan on.
 */
fun List<Entry>.lastMonthsBySource(lastMonth: YearMonth, count: Int): List<MonthBySource> =
    (count - 1 downTo 0).map { back ->
        val month = lastMonth.minusMonths(back.toLong())
        val bySource = confirmedIncome(month)
            .filter { it.incomeSource != IncomeSource.CRYPTO }
            .groupBy { it.incomeSource }
            .mapValues { (_, entries) -> entries.sumOf { it.amount } }
        MonthBySource(month, bySource)
    }

/**
 * How much of a month's income he can count on. Only the base salary is dependable;
 * the bonus, freelance, AdSense and the rest change from month to month. Crypto is
 * left out, as in the chart.
 */
data class IncomeDependability(val dependable: Long, val variable: Long) {
    val total: Long
        get() = dependable + variable

    /** Dependable share, 0 to 100; the two percentages always add up to 100. */
    val dependablePercent: Int
        get() = if (total == 0L) 0 else (dependable * 100.0 / total).roundToLong().toInt()

    val variablePercent: Int
        get() = if (total == 0L) 0 else 100 - dependablePercent
}

fun List<Entry>.dependability(month: YearMonth): IncomeDependability {
    val income = confirmedIncome(month).filter { it.incomeSource != IncomeSource.CRYPTO }
    val base = income.filter { it.isSalary }.sumOf { it.baseAmount }
    return IncomeDependability(dependable = base, variable = income.sumOf { it.amount } - base)
}

/** Crypto income confirmed from January of [year] up to now. */
fun List<Entry>.cryptoTotal(year: Int): Long =
    filter { it.type == EntryType.INCOME && !it.isExpected && it.incomeSource == IncomeSource.CRYPTO && it.date.year == year }
        .sumOf { it.amount }
