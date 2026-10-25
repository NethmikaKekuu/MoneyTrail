package lk.moneytrail.app.presentation.auth

import android.util.Patterns

// Stricter than Firebase's own 6-character minimum, to match the sign-up design.
internal const val MIN_PASSWORD_LENGTH = 8

/** Returns a message to show when [email] is not usable, or null when it looks valid. */
internal fun emailError(email: String): String? = when {
    email.isBlank() -> "Enter your email."
    !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Enter a valid email address."
    else -> null
}

/** The three rules shown under the sign-up password field. */
data class PasswordChecks(
    val isLongEnough: Boolean,
    val hasNumber: Boolean,
    val hasLetter: Boolean
) {
    val passedCount: Int
        get() = listOf(isLongEnough, hasNumber, hasLetter).count { it }

    val allPassed: Boolean
        get() = isLongEnough && hasNumber && hasLetter

    companion object {
        fun of(password: String) = PasswordChecks(
            isLongEnough = password.length >= MIN_PASSWORD_LENGTH,
            hasNumber = password.any(Char::isDigit),
            hasLetter = password.any(Char::isLetter)
        )
    }
}
