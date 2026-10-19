package lk.moneytrail.app.presentation.auth

data class LoginState(
    val email: String = "",
    val password: String = "",
    /** Email and password sign-in is running. */
    val isLoading: Boolean = false,
    /** Firebase is checking the Google account the user picked. */
    val isGoogleLoading: Boolean = false,
    val errorMessage: String? = null,
    /** Set once sign-in has finished; the screen then leaves. */
    val signedIn: SignedIn? = null
) {
    val isBusy: Boolean
        get() = isLoading || isGoogleLoading
}

enum class SignedIn {
    /** Has a profile already: go to the dashboard. */
    RETURNING_USER,

    /** First Google sign-in, profile just made: go to onboarding. */
    NEW_USER
}
