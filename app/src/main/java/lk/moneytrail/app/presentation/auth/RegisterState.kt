package lk.moneytrail.app.presentation.auth

data class RegisterState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val hasAcceptedTerms: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRegistered: Boolean = false
) {
    /** Drives the strength bars and checklist under the password field. */
    val passwordChecks: PasswordChecks
        get() = PasswordChecks.of(password)
}
