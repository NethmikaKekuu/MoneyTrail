package lk.moneytrail.app.data.repository

import android.util.Log
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

private const val SERVER_WAIT_MS = 3_000L

/**
 * Waits a short while for the server to confirm a Firestore write. Firestore puts the
 * write in its offline cache straight away, so listeners show it at once. If the server
 * is slow, after a few seconds the write is left to sync later instead of keeping the
 * user waiting; when [isOnline] is false it doesn't wait at all, as no answer can come.
 * Throws if the server rejects the write while it waits.
 */
internal suspend fun Task<Void>.awaitServerBriefly(tag: String, isOnline: Boolean) {
    addOnFailureListener { Log.w(tag, "A write failed on the server", it) }
    if (!isOnline) return
    withTimeoutOrNull(SERVER_WAIT_MS) { await() }
}
