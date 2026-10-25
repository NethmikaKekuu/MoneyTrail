package lk.moneytrail.app.presentation.auth

data class ForgotPasswordState(
    val email: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** The address the reset link went to. Non-null switches the screen to "check your email". */
    val sentTo: String? = null
)
