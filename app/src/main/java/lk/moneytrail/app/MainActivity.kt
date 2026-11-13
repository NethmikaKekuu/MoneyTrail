package lk.moneytrail.app

import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import lk.moneytrail.app.data.preferences.ThemePreferences
import lk.moneytrail.app.presentation.navigation.MoneyTrailNavHost
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themePreferences: ThemePreferences

    /** The reminder kind of a tapped phone notification, until navigation has opened it. */
    private var openedNotification by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Only a fresh start; after a rotation the reminder was already opened.
        if (savedInstanceState == null) openedNotification = intent.getStringExtra(EXTRA_NOTIFICATION_KIND)
        enableEdgeToEdge()
        setContent {
            val themeMode by themePreferences.themeMode.collectAsStateWithLifecycle()

            MoneyTrailTheme(themeMode = themeMode) {
                // Paints the theme background behind every screen, including while
                // screens cross-fade during navigation.
                Surface(modifier = Modifier.fillMaxSize()) {
                    MoneyTrailNavHost(
                        openedNotification = openedNotification,
                        onOpenedNotificationHandled = { openedNotification = null }
                    )
                }
            }
        }
    }

    /** The app is already open and a phone notification is tapped. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_NOTIFICATION_KIND)?.let { openedNotification = it }
    }

    companion object {
        /** The [lk.moneytrail.app.domain.model.NotificationKind] name a phone notification opens. */
        const val EXTRA_NOTIFICATION_KIND = "notificationKind"
    }
}
