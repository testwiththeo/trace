package com.trace.app.presentation.navigation

/**
 * Navigation routes for the app.
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object TrafficLog : Screen("traffic_log")
    object TrafficDetail : Screen("traffic_detail/{trafficId}") {
        fun createRoute(trafficId: Long) = "traffic_detail/$trafficId"
    }

    object MockRules : Screen("mock_rules")
    object AddMockRule : Screen("add_mock_rule")
    object EditMockRule : Screen("edit_mock_rule/{ruleId}") {
        fun createRoute(ruleId: Long) = "edit_mock_rule/$ruleId"
    }

    object Settings : Screen("settings")
}
