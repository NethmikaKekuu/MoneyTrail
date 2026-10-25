package lk.moneytrail.app.domain.repository

import lk.moneytrail.app.domain.model.UserProfile

// Failures are returned as an lk.moneytrail.app.domain.model.AppException whose
// message can be shown to the user.
interface UserRepository {

    suspend fun createProfile(profile: UserProfile): Result<Unit>

    /** Null when the user has no profile yet, e.g. accounts made before profiles existed. */
    suspend fun getProfile(uid: String): Result<UserProfile?>
}
