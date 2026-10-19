package lk.moneytrail.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.moneytrail.app.domain.usecase.RegisterUseCase
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterState())
    val uiState: StateFlow<RegisterState> = _uiState.asStateFlow()

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, errorMessage = null) }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun onTermsCheckedChange(accepted: Boolean) {
        _uiState.update { it.copy(hasAcceptedTerms = accepted, errorMessage = null) }
    }

    fun register() {
        val state = _uiState.value
        if (state.isLoading) return

        val error = validate(state)
        if (error != null) {
            _uiState.update { it.copy(errorMessage = error) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            registerUseCase(state.name.trim(), state.email.trim(), state.password)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isRegistered = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
        }
    }

    private fun validate(state: RegisterState): String? {
        if (state.name.isBlank()) return "Enter your name."
        emailError(state.email)?.let { return it }
        val checks = state.passwordChecks
        if (!checks.isLongEnough) return "Password must be at least $MIN_PASSWORD_LENGTH characters."
        if (!checks.allPassed) return "Password needs at least one letter and one number."
        if (!state.hasAcceptedTerms) return "Agree to the Terms and Privacy Policy to continue."
        return null
    }
}
