package lk.moneytrail.app.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lk.moneytrail.app.domain.repository.AuthRepository
import lk.moneytrail.app.presentation.navigation.Routes
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    /** Where to go once the splash is done: null while it's still showing. */
    private val _destination = MutableStateFlow<String?>(null)
    val destination: StateFlow<String?> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            // Firebase restores the session from disk instantly, so the wait is only
            // to let the brand screen be seen instead of flashing past.
            delay(SPLASH_DURATION_MS)
            _destination.value =
                if (authRepository.currentUserId != null) Routes.DASHBOARD else Routes.WELCOME
        }
    }

    private companion object {
        const val SPLASH_DURATION_MS = 900L
    }
}
