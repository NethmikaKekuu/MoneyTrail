package lk.moneytrail.app.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Whether the phone can reach the internet. Entries are read from the phone and saves
 * are queued, so the app works either way; this only lets screens say so and lets
 * saves skip waiting for a server that can't answer.
 */
interface ConnectivityRepository {

    /** True while the phone is online. Emits the current state first, then each change. */
    val isOnline: Flow<Boolean>

    /** Whether the phone is online right now. */
    fun isOnlineNow(): Boolean
}
