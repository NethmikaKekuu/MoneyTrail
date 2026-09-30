package lk.moneytrail.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import lk.moneytrail.app.ui.auth.LoginScreen
import lk.moneytrail.app.ui.auth.RegisterScreen
import lk.moneytrail.app.ui.dashboard.DashboardScreen
import lk.moneytrail.app.ui.onboarding.OnboardingScreen

@Composable
fun MoneyTrailNavHost(
    startDestination: String,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onSignedIn = { navController.navigateAndClearBackStack(Routes.DASHBOARD) },
                onCreateAccountClick = {
                    navController.navigate(Routes.REGISTER) { launchSingleTop = true }
                }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegistered = { navController.navigateAndClearBackStack(Routes.ONBOARDING) },
                onHaveAccountClick = { navController.popBackStack(Routes.LOGIN, inclusive = false) }
            )
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinishClick = { navController.navigateAndClearBackStack(Routes.DASHBOARD) }
            )
        }
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onSignedOut = { navController.navigateAndClearBackStack(Routes.LOGIN) }
            )
        }
    }
}

// Crossing the signed-in/signed-out line replaces the whole back stack, so Back
// can't return to Login after signing in, or to the Dashboard after signing out.
private fun NavController.navigateAndClearBackStack(route: String) {
    val rootId = graph.id
    navigate(route) {
        popUpTo(rootId) { inclusive = true }
    }
}
