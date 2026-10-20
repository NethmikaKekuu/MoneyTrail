package lk.moneytrail.app.data.repository

import com.google.firebase.firestore.FirebaseFirestoreException

internal const val FIRESTORE_UNREACHABLE_MESSAGE =
    "Can't reach Firestore. Check your internet connection and try again."

internal const val SIGNED_OUT_MESSAGE = "You're signed out. Sign in again."

/**
 * A message for the user about a failed Firestore call. The first two point at the
 * Firebase console, because they mean the project isn't set up yet rather than that
 * the user did something wrong. [fallback] covers everything else.
 */
internal fun Exception.toFirestoreMessage(fallback: String): String =
    when ((this as? FirebaseFirestoreException)?.code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Firestore blocked this request. Check the Rules tab of Firestore in the Firebase console."
        FirebaseFirestoreException.Code.NOT_FOUND ->
            "The Firestore database hasn't been created yet in the Firebase console."
        FirebaseFirestoreException.Code.UNAVAILABLE ->
            FIRESTORE_UNREACHABLE_MESSAGE
        else ->
            fallback
    }
