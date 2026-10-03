package lk.moneytrail.app.domain.usecase

import lk.moneytrail.app.domain.model.UserProfile
import lk.moneytrail.app.domain.repository.AuthRepository
import lk.moneytrail.app.domain.repository.UserRepository
import javax.inject.Inject

/**
 * Creates the Firebase account and its users/{uid} profile together. If the profile
 * can't be saved, the new account is deleted again so that trying again doesn't fail
 * with "this email already exists".
 */
class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(name: String, email: String, password: String): Result<Unit> {
        val uid = authRepository.register(email, password)
            .getOrElse { return Result.failure(it) }

        return userRepository.createProfile(UserProfile(uid = uid, name = name, email = email))
            .onFailure { authRepository.deleteCurrentUser() }
    }
}
