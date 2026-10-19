package lk.moneytrail.app.presentation.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

/** What came back from the Google account picker. */
sealed interface GoogleIdTokenResult {
    data class Success(val idToken: String) : GoogleIdTokenResult

    /** The user closed the picker. Not an error, so nothing is shown. */
    data object Cancelled : GoogleIdTokenResult

    data class Failure(val message: String) : GoogleIdTokenResult
}

private const val TAG = "GoogleSignIn"
private const val GENERIC_MESSAGE = "Couldn't sign in with Google. Please try again."

/**
 * Shows the Google account picker and returns the picked account's ID token.
 *
 * This lives in the UI layer because Credential Manager needs the screen's Activity
 * [context] to show its sheet, the same way a screen launches an activity for a
 * result. It only fetches the token; the ViewModel signs in to Firebase with it.
 *
 * [webClientId] is the OAuth "Web client" from google-services.json, which the
 * google-services plugin turns into R.string.default_web_client_id.
 */
internal suspend fun requestGoogleIdToken(context: Context, webClientId: String): GoogleIdTokenResult {
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(GetSignInWithGoogleOption.Builder(serverClientId = webClientId).build())
        .build()

    return try {
        val credential = CredentialManager.create(context).getCredential(context, request).credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            GoogleIdTokenResult.Success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
        } else {
            Log.w(TAG, "Unexpected credential type: ${credential.type}")
            GoogleIdTokenResult.Failure(GENERIC_MESSAGE)
        }
    } catch (e: GetCredentialCancellationException) {
        GoogleIdTokenResult.Cancelled
    } catch (e: NoCredentialException) {
        Log.w(TAG, "No Google account available", e)
        GoogleIdTokenResult.Failure("No Google account found on this phone. Add one in Settings and try again.")
    } catch (e: GetCredentialException) {
        Log.w(TAG, "Google account picker failed", e)
        GoogleIdTokenResult.Failure(e.toUserMessage())
    } catch (e: GoogleIdTokenParsingException) {
        Log.w(TAG, "Couldn't read the Google ID token", e)
        GoogleIdTokenResult.Failure(GENERIC_MESSAGE)
    }
}

// A missing or wrong SHA-1 fingerprint in the Firebase console shows up as this
// message from Google Play services, so say what to fix instead of "try again".
private fun GetCredentialException.toUserMessage(): String =
    if (message.orEmpty().contains("Developer console is not set up correctly")) {
        "Google sign-in isn't set up for this build. Add its SHA-1 fingerprint in the Firebase console."
    } else {
        GENERIC_MESSAGE
    }
