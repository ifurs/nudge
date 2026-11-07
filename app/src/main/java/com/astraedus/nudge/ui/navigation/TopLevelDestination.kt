package com.astraedus.nudge.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.astraedus.nudge.R

/**
 * The four places the bottom bar switches between. A destination is top-level when it is one of
 * these; every other route (an app's config, the group editor, Nuke, Willpower, onboarding...) is a
 * detail screen that keeps its own back-arrow top bar and no bottom bar.
 *
 * One list, so the bar, the "show the bar?" question and the widget deep link all read the same
 * routes. The routes are [Screen]'s own, never retyped.
 */
enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    Home(Screen.Home.route, R.string.nav_home, Icons.Outlined.Home, Icons.Filled.Home),
    Apps(Screen.Apps.route, R.string.nav_apps, Icons.Outlined.Apps, Icons.Filled.Apps),
    Stats(Screen.Stats.route, R.string.nav_stats, Icons.Outlined.BarChart, Icons.Filled.BarChart),
    Settings(Screen.Settings.route, R.string.nav_settings, Icons.Outlined.Settings, Icons.Filled.Settings);

    companion object {
        /** The destination whose route is [route], or null for a detail screen (or no route yet). */
        fun fromRoute(route: String?): TopLevelDestination? = entries.firstOrNull { it.route == route }

        /** Whether the bottom bar is shown on [route]. */
        fun showsBottomBar(route: String?): Boolean = fromRoute(route) != null
    }
}
