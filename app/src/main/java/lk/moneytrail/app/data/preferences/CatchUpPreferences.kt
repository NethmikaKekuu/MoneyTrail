package lk.moneytrail.app.data.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import lk.moneytrail.app.domain.repository.CatchUpRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Keeps the catch-up dates on the phone, like the theme; they only decide when to offer it. */
@Singleton
class CatchUpPreferences @Inject constructor(
    @ApplicationContext context: Context
) : CatchUpRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun lastCatchUpOn(): LocalDate? = readDate(KEY_LAST_CATCH_UP)

    override fun setLastCatchUpOn(date: LocalDate) = writeDate(KEY_LAST_CATCH_UP, date)

    override fun snoozedOn(): LocalDate? = readDate(KEY_SNOOZED_ON)

    override fun setSnoozedOn(date: LocalDate) = writeDate(KEY_SNOOZED_ON, date)

    private fun readDate(key: String): LocalDate? =
        prefs.getString(key, null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    private fun writeDate(key: String, date: LocalDate) {
        prefs.edit().putString(key, date.toString()).apply()
    }

    private companion object {
        const val PREFS_NAME = "moneytrail_catch_up"
        const val KEY_LAST_CATCH_UP = "last_catch_up_on"
        const val KEY_SNOOZED_ON = "snoozed_on"
    }
}
