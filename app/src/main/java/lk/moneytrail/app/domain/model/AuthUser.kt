package lk.moneytrail.app.domain.model

/** The signed-in Firebase user. [displayName] comes from the Google account, if there is one. */
data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?
)
