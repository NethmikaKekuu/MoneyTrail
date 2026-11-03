package lk.moneytrail.app.presentation.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.moneytrail.app.domain.model.NotificationKind
import lk.moneytrail.app.domain.repository.ConnectivityRepository
import lk.moneytrail.app.domain.repository.NotificationLogRepository
import lk.moneytrail.app.domain.usecase.CheckCatchUpUseCase
import lk.moneytrail.app.domain.usecase.RefreshNotificationsUseCase
import lk.moneytrail.app.presentation.navigation.Routes
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val connectivityRepository: ConnectivityRepository,
    private val checkCatchUp: CheckCatchUpUseCase,
    private val notificationLog: NotificationLogRepository,
    private val refreshNotifications: RefreshNotificationsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardState())
    val uiState: StateFlow<DashboardState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Back after a few days away: offer the catch-up as the app opens.
            if (checkCatchUp(LocalDate.now()) != null) {
                _uiState.update { it.copy(openCatchUp = true) }
            }
        }
        viewModelScope.launch {
            savedStateHandle.getStateFlow<String?>(Routes.RESULT_NOTIFICATION_ACTION, null).collect { name ->
                val kind = NotificationKind.entries.find { it.name == name }
                _uiState.update { it.copy(notificationAction = kind) }
                // Opened, from the list or the phone: it's read now, and the others stay as they are.
                kind?.let { notificationLog.markKindRead(it) }
            }
        }
        viewModelScope.launch {
            notificationLog.observeUnread().collect { items ->
                _uiState.update { it.copy(unreadNotifications = items.size) }
            }
        }
        viewModelScope.launch {
            connectivityRepository.isOnline.collect { isOnline ->
                _uiState.update { it.copy(isOffline = !isOnline) }
            }
        }
    }

    /** The catch-up has opened, so it isn't opened again while the app stays open. */
    fun onCatchUpOpened() {
        _uiState.update { it.copy(openCatchUp = false) }
    }

    /** The dashboard has acted on the chosen reminder action. */
    fun onNotificationActionHandled() {
        savedStateHandle[Routes.RESULT_NOTIFICATION_ACTION] = null
    }

    /** The dashboard is shown again: reminders that were dealt with meanwhile leave the notification list. */
    fun onResume() {
        viewModelScope.launch { refreshNotifications(LocalDate.now()) }
    }
}
