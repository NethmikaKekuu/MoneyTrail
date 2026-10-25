package lk.moneytrail.app.domain.model

import java.time.Instant

/** The users/{uid} document in Firestore. */
data class UserProfile(
    val uid: String,
    val name: String,
    val email: String,
    /** Set by the Firestore server when the profile is first saved; null before that. */
    val createdAt: Instant? = null
)
