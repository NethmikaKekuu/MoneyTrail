package lk.moneytrail.app.presentation.notifications

import lk.moneytrail.app.domain.model.AppNotification
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
    private val notificationLog: NotificationLogRepository,
    private val refreshNotifications: RefreshNotificationsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsState())
    val uiState: StateFlow<NotificationsState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            notificationLog.observeAll().collect { items ->
                _uiState.update { it.copy(items = items, isLoading = false) }
            }
        }
    }

    /** The screen is shown again, e.g. back from doing what a reminder asked: clear what's now done. */
    fun onResume() {
        viewModelScope.launch { refreshNotifications(LocalDate.now()) }
    }

    /** A reminder is opened: only that one becomes read; the rest of the list stays as it is. */
    fun onItemClick(item: AppNotification) {
        if (!item.isRead) viewModelScope.launch { notificationLog.markRead(item.id) }
    }
}
