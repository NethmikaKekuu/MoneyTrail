package lk.moneytrail.app.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.moneytrail.app.domain.repository.NotificationLogRepository
import lk.moneytrail.app.domain.usecase.RefreshNotificationsUseCase
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    notificationLog: NotificationLogRepository,
    private val refreshNotifications: RefreshNotificationsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsState())
    val uiState: StateFlow<NotificationsState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            notificationLog.observeUnread().collect { items ->
                _uiState.update { it.copy(items = items, isLoading = false) }
            }
        }
    }

    /** The screen is shown again, e.g. back from doing what a reminder asked: clear what's now done. */
    fun onResume() {
        viewModelScope.launch { refreshNotifications(LocalDate.now()) }
    }

    fun onItemClick(id: Long) {
        _uiState.update { it.copy(selectedId = id) }
    }

    /** "Later": closes the sheet and leaves the reminder unread. */
    fun onLaterClick() {
        _uiState.update { it.copy(selectedId = null) }
    }
}
