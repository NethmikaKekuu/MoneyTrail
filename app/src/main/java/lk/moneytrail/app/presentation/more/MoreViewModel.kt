package lk.moneytrail.app.presentation.more

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

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MoreState>(MoreState.Loading)
    val uiState: StateFlow<MoreState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        val uid = authRepository.currentUserId
        if (uid == null) {
            _uiState.value = MoreState.Error("You're signed out. Sign in again.")
            return
        }

        _uiState.value = MoreState.Loading
        viewModelScope.launch {
            userRepository.getProfile(uid)
                .onSuccess { profile ->
                    _uiState.value = MoreState.Ready(
                        name = profile?.name,
                        email = profile?.email ?: authRepository.currentUserEmail
                    )
                }
                .onFailure { e ->
                    _uiState.value = MoreState.Error(
                        e.message ?: "Couldn't load your profile."
                    )
                }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }
}
