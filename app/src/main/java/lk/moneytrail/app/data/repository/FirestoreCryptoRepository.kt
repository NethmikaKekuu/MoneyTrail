package lk.moneytrail.app.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import lk.moneytrail.app.domain.model.AppException
import lk.moneytrail.app.domain.model.crypto.CryptoTrade
import lk.moneytrail.app.domain.model.crypto.TradeSide
import lk.moneytrail.app.domain.repository.ConnectivityRepository
import lk.moneytrail.app.domain.repository.CryptoRepository
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Trades live in users/{uid}/cryptoTrades/{tradeKey}. The key comes from the trade
 * itself, so importing the same export twice overwrites rather than doubles.
 */
@Singleton
class FirestoreCryptoRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val connectivity: ConnectivityRepository
) : CryptoRepository {

    override suspend fun getExistingTrades(): List<CryptoTrade> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return tradesOf(uid).get().await().documents.mapNotNull { it.toTrade() }
    }

    override fun observeTrades(): Flow<Result<List<CryptoTrade>>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Result.failure(AppException(SIGNED_OUT_MESSAGE)))
            close()
            awaitClose()
            return@callbackFlow
        }
        val registration = tradesOf(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Listening to crypto trades failed", error)
                trySend(Result.failure(AppException(error.toFirestoreMessage(LOAD_FAILED_MESSAGE), error)))
                close()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(Result.success(snapshot.documents.mapNotNull { it.toTrade() }.sortedBy { it.date }))
            }
        }
        awaitClose { registration.remove() }
    }

    override suspend fun saveTrades(trades: List<CryptoTrade>): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(AppException(SIGNED_OUT_MESSAGE))
        return try {
            val online = connectivity.isOnlineNow()
            // A batch holds at most 500 writes.
            trades.chunked(BATCH_SIZE).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { trade -> batch.set(tradesOf(uid).document(trade.key()), trade.toFirestore()) }
                batch.commit().awaitServerBriefly(TAG, online)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Saving crypto trades failed", e)
            Result.failure(AppException(e.toFirestoreMessage(SAVE_FAILED_MESSAGE), e))
        }
    }

    override suspend fun deleteAllTrades(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(AppException(SIGNED_OUT_MESSAGE))
        return try {
            val online = connectivity.isOnlineNow()
            tradesOf(uid).get().await().documents.chunked(BATCH_SIZE).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().awaitServerBriefly(TAG, online)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Deleting crypto trades failed", e)
            Result.failure(AppException(e.toFirestoreMessage(DELETE_FAILED_MESSAGE), e))
        }
    }

    private fun tradesOf(uid: String): CollectionReference =
        firestore.collection("users").document(uid).collection("cryptoTrades")

    private companion object {
        const val TAG = "CryptoRepository"
        const val BATCH_SIZE = 400
        const val SIGNED_OUT_MESSAGE = "You're signed out. Sign in again to see your crypto trades."
        const val LOAD_FAILED_MESSAGE = "Couldn't load your crypto trades."
        const val SAVE_FAILED_MESSAGE = "Couldn't save the imported trades."
        const val DELETE_FAILED_MESSAGE = "Couldn't remove the imported trades."
    }
}

/** Binance's trade id when there is one, else a stable id from the trade's own details. */
internal fun CryptoTrade.key(): String =
    if (tradeId.isNotBlank()) "id_" + tradeId.trim().replace("/", "_")
    else "t_" + UUID.nameUUIDFromBytes("$date|${pair.uppercase(Locale.US)}|$type|$amount|$price".toByteArray())

private fun CryptoTrade.toFirestore(): Map<String, Any?> = mapOf(
    "tradeId" to tradeId,
    "date" to date.toString(),
    "pair" to pair,
    "side" to type.name,
    "price" to price,
    "amount" to amount,
    "total" to total,
    "fee" to fee,
    "feeCoin" to feeCoin,
    "costBasis" to costBasis,
    "importedAt" to FieldValue.serverTimestamp()
)

private fun DocumentSnapshot.toTrade(): CryptoTrade? {
  return try {
    CryptoTrade(
        tradeId = getString("tradeId").orEmpty(),
        date = LocalDateTime.parse(getString("date")),
        pair = getString("pair") ?: return null,
        type = TradeSide.valueOf(getString("side") ?: return null),
        price = getDouble("price") ?: return null,
        amount = getDouble("amount") ?: return null,
        total = getDouble("total") ?: 0.0,
        fee = getDouble("fee") ?: 0.0,
        feeCoin = getString("feeCoin").orEmpty(),
        costBasis = getDouble("costBasis")
    )
} catch (e: Exception) {
    Log.w("CryptoRepository", "Skipping unreadable trade $id", e)
    null
  }
}
