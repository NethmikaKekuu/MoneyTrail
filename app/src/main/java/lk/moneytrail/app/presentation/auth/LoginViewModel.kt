package lk.moneytrail.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.moneytrail.app.domain.repository.AuthRepository
import lk.moneytrail.app.domain.usecase.SignInWithGoogleUseCase
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginState())
    val uiState: StateFlow<LoginState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun signIn() {
        val state = _uiState.value
        if (state.isBusy) return

        val error = emailError(state.email)
            ?: if (state.password.isEmpty()) "Enter your password." else null
        if (error != null) {
            _uiState.update { it.copy(errorMessage = error) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            authRepository.signIn(state.email.trim(), state.password)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, signedIn = SignedIn.RETURNING_USER) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
        }
    }

    /** Called with whatever the Google account picker returned. */
    fun onGoogleIdTokenResult(result: GoogleIdTokenResult) {
        when (result) {
            GoogleIdTokenResult.Cancelled -> Unit
            is GoogleIdTokenResult.Failure -> _uiState.update { it.copy(errorMessage = result.message) }
            is GoogleIdTokenResult.Success -> signInWithGoogle(result.idToken)
        }
    }

    private fun signInWithGoogle(idToken: String) {
        if (_uiState.value.isBusy) return

        _uiState.update { it.copy(isGoogleLoading = true, errorMessage = null) }
        viewModelScope.launch {
            signInWithGoogleUseCase(idToken)
                .onSuccess { isNewUser ->
                    _uiState.update {
                        it.copy(
                            isGoogleLoading = false,
                            signedIn = if (isNewUser) SignedIn.NEW_USER else SignedIn.RETURNING_USER
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isGoogleLoading = false, errorMessage = e.message) }
                }
        }
    }
}
