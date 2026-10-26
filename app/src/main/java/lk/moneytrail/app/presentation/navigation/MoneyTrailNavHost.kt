package lk.moneytrail.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import lk.moneytrail.app.presentation.auth.ForgotPasswordScreen
import lk.moneytrail.app.presentation.auth.LoginScreen
import lk.moneytrail.app.presentation.auth.RegisterScreen
import lk.moneytrail.app.presentation.catchup.CatchUpScreen
import lk.moneytrail.app.presentation.dashboard.DashboardScreen
import lk.moneytrail.app.presentation.entry.AddEntryScreen
import lk.moneytrail.app.presentation.entry.EditEntryScreen
import lk.moneytrail.app.presentation.freelance.AddProjectScreen
import lk.moneytrail.app.presentation.freelance.FreelanceProjectsScreen
import lk.moneytrail.app.presentation.goal.GoalScreen
import lk.moneytrail.app.presentation.onboarding.OnboardingScreen
import lk.moneytrail.app.presentation.salary.SalaryScreen
import lk.moneytrail.app.presentation.settings.SettingsScreen
import lk.moneytrail.app.presentation.splash.SplashScreen
import lk.moneytrail.app.presentation.welcome.WelcomeScreen
import java.time.LocalDate

@Composable
fun MoneyTrailNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onFinished = { route -> navController.navigateAndClearBackStack(route) }
            )
        }
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onCreateAccountClick = { navController.navigate(Routes.REGISTER) { launchSingleTop = true } },
                onLogInClick = { navController.navigate(Routes.LOGIN) { launchSingleTop = true } }
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onSignedIn = { navController.navigateAndClearBackStack(Routes.DASHBOARD) },
                onNewGoogleUser = { navController.navigateAndClearBackStack(Routes.ONBOARDING) },
                onCreateAccountClick = { navController.switchAuthScreen(Routes.REGISTER) },
                onForgotPasswordClick = {
                    navController.navigate(Routes.FORGOT_PASSWORD) { launchSingleTop = true }
                }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegistered = { navController.navigateAndClearBackStack(Routes.ONBOARDING) },
                onBackClick = { navController.popBackStack() },
                onLogInClick = { navController.switchAuthScreen(Routes.LOGIN) }
            )
        }
        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinishClick = { navController.navigateAndClearBackStack(Routes.DASHBOARD) }
            )
        }
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onAddEntryClick = { date ->
                    navController.navigate(Routes.addEntry(date)) { launchSingleTop = true }
                },
                onEntryClick = { entryId ->
                    navController.navigate(Routes.editEntry(entryId)) { launchSingleTop = true }
                },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) { launchSingleTop = true } },
                onSalaryClick = { navController.navigate(Routes.SALARY) { launchSingleTop = true } },
                onCatchUpClick = { navController.navigate(Routes.CATCH_UP) { launchSingleTop = true } },
                onFreelanceClick = { navController.navigate(Routes.FREELANCE) { launchSingleTop = true } },
                onSignedOut = { navController.navigateAndClearBackStack(Routes.LOGIN) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBackClick = { navController.popBackStack(Routes.SETTINGS, inclusive = true) }
            )
        }
        composable(Routes.CATCH_UP) {
            CatchUpScreen(
                onAddEntryClick = { navController.navigate(Routes.addEntry(LocalDate.now())) { launchSingleTop = true } },
                onFinished = { navController.popBackStack(Routes.CATCH_UP, inclusive = true) }
            )
        }
        composable(Routes.GOAL) {
            GoalScreen(
                onBackClick = { navController.popBackStack(Routes.GOAL, inclusive = true) }
            )
        }
        composable(Routes.SALARY) {
            SalaryScreen(
                onBackClick = { navController.popBackStack(Routes.SALARY, inclusive = true) },
                // Onboarding ends on a fresh dashboard, which then expects the new salary.
                onSetUpClick = { navController.navigate(Routes.ONBOARDING) { launchSingleTop = true } }
            )
        }
        composable(Routes.FREELANCE) {
            FreelanceProjectsScreen(
                onBackClick = { navController.popBackStack(Routes.FREELANCE, inclusive = true) },
                onAddProjectClick = { navController.navigate(Routes.ADD_PROJECT) { launchSingleTop = true } }
            )
        }
        composable(Routes.ADD_PROJECT) {
            AddProjectScreen(
                onBackClick = { navController.popBackStack(Routes.ADD_PROJECT, inclusive = true) },
                onProjectSaved = { navController.popBackStack(Routes.ADD_PROJECT, inclusive = true) }
            )
        }
        composable(
            route = Routes.ADD_ENTRY,
            arguments = listOf(
                navArgument(Routes.ADD_ENTRY_DATE_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            AddEntryScreen(
                // Pops only this screen, so saving and tapping close together can't also
                // pop the dashboard.
                onClose = { navController.popBackStack(Routes.ADD_ENTRY, inclusive = true) }
            )
        }
        composable(
            route = Routes.EDIT_ENTRY,
            arguments = listOf(navArgument(Routes.EDIT_ENTRY_ID_ARG) { type = NavType.StringType })
        ) {
            EditEntryScreen(
                // Pops only this screen, like the new entry screen does.
                onClose = { navController.popBackStack(Routes.EDIT_ENTRY, inclusive = true) }
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

// Login and Register link to each other. Go back to the other one when it's already
// on the back stack, so switching back and forth doesn't pile up screens.
private fun NavController.switchAuthScreen(route: String) {
    if (!popBackStack(route, inclusive = false)) {
        navigate(route) { launchSingleTop = true }
    }
}
