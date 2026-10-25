package lk.moneytrail.app.presentation.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.moneytrail.app.data.preferences.ThemeMode
import lk.moneytrail.app.data.preferences.ThemePreferences
import lk.moneytrail.app.domain.model.GoalModel
import lk.moneytrail.app.domain.repository.AuthRepository
import lk.moneytrail.app.domain.repository.PlanRepository
import lk.moneytrail.app.domain.repository.UserRepository
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val planRepository: PlanRepository,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<MoreState>(MoreState.Loading)
    val uiState: StateFlow<MoreState> = _uiState.asStateFlow()

    init {
        loadProfile()
        observeThemeMode()
    }

    private fun observeThemeMode() {
        viewModelScope.launch {
            themePreferences.themeMode.collect { mode ->
                _uiState.update { state ->
                    if (state is MoreState.Ready) {
                        state.copy(themeMode = mode)
                    } else state
                }
            }
        }
    }

    fun loadProfile() {
        val uid = authRepository.currentUserId
        if (uid == null) {
            _uiState.value = MoreState.Error("You're signed out. Sign in again.")
            return
        }

        _uiState.value = MoreState.Loading
        viewModelScope.launch {
            val plan = planRepository.getPlan().getOrNull()
            val goalBadge = plan?.goal?.let { goal ->
                GoalModel.fromSavingsGoal(goal, LocalDate.now()).percentageText
            } ?: "2.3%"

            userRepository.getProfile(uid)
                .onSuccess { profile ->
                    _uiState.value = MoreState.Ready(
                        name = profile?.name,
                        email = profile?.email ?: authRepository.currentUserEmail,
                        goalProgressBadge = goalBadge,
                        themeMode = themePreferences.getThemeMode()
                    )
                }
                .onFailure { e ->
                    _uiState.value = MoreState.Error(
                        e.message ?: "Couldn't load your profile."
                    )
                }
        }
    }

    fun onOpenThemeDialog() {
        _uiState.update {
            if (it is MoreState.Ready) {
                it.copy(isThemeDialogOpen = true)
            } else it
        }
    }

    fun onDismissThemeDialog() {
        _uiState.update {
            if (it is MoreState.Ready) {
                it.copy(isThemeDialogOpen = false)
            } else it
        }
    }

    fun onSelectThemeMode(mode: ThemeMode) {
        themePreferences.setThemeMode(mode)
        _uiState.update {
            if (it is MoreState.Ready) {
                it.copy(isThemeDialogOpen = false, themeMode = mode)
            } else it
        }
    }

    fun signOut() {
        authRepository.signOut()
    }
}
