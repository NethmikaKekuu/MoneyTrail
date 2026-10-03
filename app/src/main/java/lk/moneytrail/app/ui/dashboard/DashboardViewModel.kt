package lk.moneytrail.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lk.moneytrail.app.domain.repository.AuthRepository
import lk.moneytrail.app.domain.repository.UserRepository
import javax.inject.Inject

sealed interface DashboardUiState {
    data object Loading : DashboardUiState

    /** [name] is null when the account has no Firestore profile. */
    data class Ready(val name: String?, val email: String?) : DashboardUiState

    data class Error(val message: String) : DashboardUiState
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        val uid = authRepository.currentUserId
        if (uid == null) {
            _uiState.value = DashboardUiState.Error("You're signed out. Sign in again.")
            return
        }

        _uiState.value = DashboardUiState.Loading
        viewModelScope.launch {
            userRepository.getProfile(uid)
                .onSuccess { profile ->
                    _uiState.value = DashboardUiState.Ready(
                        name = profile?.name,
                        email = profile?.email ?: authRepository.currentUserEmail
                    )
                }
                .onFailure { e ->
                    _uiState.value = DashboardUiState.Error(
                        e.message ?: "Couldn't load your profile."
                    )
                }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }
}
