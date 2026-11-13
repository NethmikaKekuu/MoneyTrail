package lk.moneytrail.app.presentation.usd

import lk.moneytrail.app.domain.model.UsdConversion
import lk.moneytrail.app.domain.repository.MarketRate
import java.time.LocalDate

/** Where the suggested market rate for the chosen day has got to. */
sealed interface SuggestedRate {
    data object Loading : SuggestedRate
    data class Found(val rate: MarketRate) : SuggestedRate
    data class Failed(val message: String) : SuggestedRate
}

/** The AdSense payment form and the numbers worked out from it. */
data class UsdPayoutState(
    /** Dollars as typed, e.g. "38.00". */
    val usdText: String = "",
    /** The day the money was received; it becomes the entry's date. */
    val date: LocalDate,
    /** Rupees typed over the automatic amount, digits only; null while the amount follows the rate. */
    val lkrOverride: String? = null,
    val suggested: SuggestedRate = SuggestedRate.Loading,
    /** True once "Type my own" is chosen; [ownRateText] then replaces the suggestion. */
    val isOwnRate: Boolean = false,
    val ownRateText: String = "",
    val accountName: String = "Main bank",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isSaved: Boolean = false
) {
    val usdCents: Long get() = UsdConversion.parseHundredths(usdText) ?: 0L

    /** The rupees: what was typed, or else the dollars at the market rate. */
    val lkr: Long
        get() = lkrOverride?.toLongOrNull() ?: UsdConversion.lkrAtRate(usdCents, marketRate)

    /** What the rupee field shows. */
    val lkrText: String get() = lkrOverride ?: lkr.takeIf { it > 0 }?.toString().orEmpty()

    /** True while the rupees are worked out from the rate rather than typed. */
    val isLkrAutomatic: Boolean get() = lkrOverride == null

    /** The market rate in use, in hundredths: the one typed, or the suggestion. 0 when there is none. */
    val marketRate: Long
        get() = when {
            isOwnRate -> UsdConversion.parseHundredths(ownRateText) ?: 0L
            suggested is SuggestedRate.Found -> suggested.rate.rate
            else -> 0L
        }

    /** Rupees lost against the market rate when the credited amount was typed lower; 0 otherwise. */
    val fee: Long get() = UsdConversion.fee(usdCents, marketRate, lkr)

    val canSave: Boolean get() = usdCents > 0 && lkr > 0 && !isSaving
}
