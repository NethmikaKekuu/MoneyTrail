package lk.moneytrail.app

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import lk.moneytrail.app.domain.repository.AuthRepository
import lk.moneytrail.app.ui.navigation.Routes
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    authRepository: AuthRepository
) : ViewModel() {

    // Decided once at launch; later sign-in/sign-out moves are handled by the NavHost.
    // Kept in the ViewModel so rotating the phone doesn't reset the start screen.
    val startDestination: String =
        if (authRepository.currentUserId != null) Routes.DASHBOARD else Routes.LOGIN
}
