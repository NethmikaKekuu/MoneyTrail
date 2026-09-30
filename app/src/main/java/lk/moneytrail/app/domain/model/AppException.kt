package lk.moneytrail.app.domain.model

/** A failure from a repository. [message] is written for the user and safe to show on screen. */
class AppException(message: String, cause: Throwable? = null) : Exception(message, cause)
