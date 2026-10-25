package lk.moneytrail.app.domain.usecase

import lk.moneytrail.app.domain.model.UserProfile
import lk.moneytrail.app.domain.repository.AuthRepository
import lk.moneytrail.app.domain.repository.UserRepository
import javax.inject.Inject

/**
 * Signs in with a Google ID token. Someone using Google for the first time gets a
 * users/{uid} profile made from their Google name and email, and the result is true
 * so they can be sent to onboarding. Returning users get false.
 *
 * If the profile can't be read or saved, the user is signed out again so the app
 * never opens on an account without a profile. Trying again finishes the setup.
 */
class SignInWithGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(idToken: String): Result<Boolean> {
        val user = authRepository.signInWithGoogle(idToken)
            .getOrElse { return Result.failure(it) }

        val existingProfile = userRepository.getProfile(user.uid)
            .getOrElse {
                authRepository.signOut()
                return Result.failure(it)
            }
        if (existingProfile != null) return Result.success(false)

        val profile = UserProfile(
            uid = user.uid,
            name = user.displayName.orEmpty(),
            email = user.email.orEmpty()
        )
        return userRepository.createProfile(profile)
            .map { true }
            .onFailure { authRepository.signOut() }
    }
}
