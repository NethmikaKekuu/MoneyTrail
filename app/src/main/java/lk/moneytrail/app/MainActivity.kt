package lk.moneytrail.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import lk.moneytrail.app.presentation.navigation.MoneyTrailNavHost
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoneyTrailTheme {
                // The window background is orange for the launch screen, so give the
                // app its own background to stop it showing through screen transitions.
                Surface(modifier = Modifier.fillMaxSize()) {
                    MoneyTrailNavHost()
                }
            }
        }
    }
}
