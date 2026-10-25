package lk.moneytrail.app.presentation.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.repository.AuthRepository
import lk.moneytrail.app.domain.repository.EntryRepository
import lk.moneytrail.app.domain.repository.UserRepository
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val entryRepository: EntryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MoreState>(MoreState.Loading)
    val uiState: StateFlow<MoreState> = _uiState.asStateFlow()

    /** The Confirm salary badge, kept so a profile loaded later still shows it. */
    private var salaryBadge: String? = null

    init {
        loadProfile()
        listenToSalary()
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
                        email = profile?.email ?: authRepository.currentUserEmail,
                        salaryBadge = salaryBadge
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

    /** This month's salary, for the badge on Confirm salary, as in the Figma More screen. */
    private fun listenToSalary() {
        viewModelScope.launch {
            val today = LocalDate.now()
            entryRepository.observeMonth(YearMonth.from(today)).collect { result ->
                // Only the badge depends on this, so a failure just leaves it out.
                result.onSuccess { entries ->
                    salaryBadge = entries.lastOrNull { it.isSalary }?.let { salaryBadgeFor(it, today) }
                    _uiState.update { state ->
                        if (state is MoreState.Ready) state.copy(salaryBadge = salaryBadge) else state
                    }
                }
            }
        }
    }

    private fun salaryBadgeFor(salary: Entry, today: LocalDate): String = when {
        !salary.isExpected -> "Confirmed"
        salary.date == today -> "Due today"
        salary.date.isBefore(today) -> "Overdue"
        else -> "Due ${salary.date.format(BadgeFormatter)}"
    }

    private companion object {
        val BadgeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    }
}
