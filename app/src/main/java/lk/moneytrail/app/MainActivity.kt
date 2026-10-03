package lk.moneytrail.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import lk.moneytrail.app.ui.navigation.MoneyTrailNavHost
import lk.moneytrail.app.ui.theme.MoneyTrailTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoneyTrailTheme {
                MoneyTrailNavHost(startDestination = viewModel.startDestination)
            }
        }
    }
}
