package com.trace.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.trace.app.presentation.screen.home.HomeScreen
import com.trace.app.presentation.screen.mockrules.MockRulesScreen
import com.trace.app.presentation.screen.settings.SettingsScreen
import com.trace.app.presentation.screen.trafficdetail.TrafficDetailScreen
import com.trace.app.presentation.screen.trafficlog.TrafficLogScreen

/**
 * Navigation graph for the Trace app.
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToTrafficLog = {
                    navController.navigate(Screen.TrafficLog.route)
                },
                onNavigateToMockRules = {
                    navController.navigate(Screen.MockRules.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.TrafficLog.route) {
            TrafficLogScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDetail = { trafficId ->
                    navController.navigate(Screen.TrafficDetail.createRoute(trafficId))
                }
            )
        }

        composable(
            route = Screen.TrafficDetail.route,
            arguments = listOf(
                navArgument("trafficId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val trafficId = backStackEntry.arguments?.getLong("trafficId") ?: 0L
            TrafficDetailScreen(
                trafficId = trafficId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.MockRules.route) {
            MockRulesScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
