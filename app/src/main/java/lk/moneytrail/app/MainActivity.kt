package lk.moneytrail.app

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by themePreferences.themeMode.collectAsStateWithLifecycle()

            MoneyTrailTheme(themeMode = themeMode) {
                // Paints the theme background behind every screen, including while
                // screens cross-fade during navigation.
                Surface(modifier = Modifier.fillMaxSize()) {
                    MoneyTrailNavHost()
                }
            }
        }
    }
}
