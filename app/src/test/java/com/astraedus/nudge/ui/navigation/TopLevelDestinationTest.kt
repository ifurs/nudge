package com.astraedus.nudge.ui.navigation

import com.astraedus.nudge.testing.SourceScan
import com.astraedus.nudge.ui.widget.WidgetDeepLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import androidx.compose.ui.unit.sp
import org.junit.Test

/** Which screens the bottom bar belongs to, and that the widget deep links still land on real ones. */
class TopLevelDestinationTest {

    @Test
    fun `the bar has the four tabs in order, on the graph's own routes`() {
        assertEquals(
            listOf(Screen.Home.route, Screen.Apps.route, Screen.Stats.route, Screen.Settings.route),
            TopLevelDestination.entries.map { it.route }
        )
    }

    @Test
    fun `the bar shows on the four tabs and nowhere else`() {
        TopLevelDestination.entries.forEach {
            assertTrue("${it.route} is a tab", TopLevelDestination.showsBottomBar(it.route))
        }
        listOf(
            Screen.Onboarding.route,
            Screen.Willpower.route,
            Screen.Interventions.route,
            Screen.GrayscaleGuide.route,
            Screen.MessagesEditor.route,
            Screen.Nuke.route,
            Screen.AppConfig.route,
            Screen.AppDetail.route,
            Screen.GroupEditor.route,
            Screen.AllAppsEditor.route,
            Screen.AppConfig.createRoute("com.instagram.android"),
            null
        ).forEach { route ->
            assertFalse("$route is a detail screen: no bottom bar", TopLevelDestination.showsBottomBar(route))
        }
    }

    @Test
    fun `every route in the graph is either a tab or a detail screen, so the bar can not miss one`() {
        val declared = Regex("""data object \w+ : Screen\("([^"]+)"\)""")
            .findAll(SourceScan.code(SourceScan.kotlinFile("com/astraedus/nudge/ui/navigation/NavGraph.kt")))
            .map { it.groupValues[1] }
            .toList()
        assertTrue("found the Screen routes", declared.size >= 10)
        val tabs = declared.filter { TopLevelDestination.showsBottomBar(it) }
        assertEquals(
            "exactly the four tabs show a bar",
            TopLevelDestination.entries.map { it.route }.toSet(),
            tabs.toSet()
        )
    }

    /**
     * Shape bug: Stats and Settings are tabs, and a back arrow on a tab pops to wherever the user
     * happened to be before (or leaves the app). Nothing but the call site decides it.
     */
    @Test
    fun `the tab screens are composed without a back arrow`() {
        val graph = SourceScan.code(SourceScan.kotlinFile("com/astraedus/nudge/ui/navigation/NavGraph.kt"))
        listOf("StatsScreen", "SettingsScreen").forEach { screen ->
            val call = graph.substringAfter("$screen(").substringBefore("onNavigateTo")
            assertTrue("$screen is a tab: onNavigateBack = null", call.contains("onNavigateBack = null"))
        }
    }

    @Test
    fun `a widget or alert deep link into a tab is entered as a tab, the rest on top`() {
        assertEquals(TopLevelDestination.Stats, TopLevelDestination.fromRoute(WidgetDeepLink.routeFor("stats")))
        assertEquals(TopLevelDestination.Settings, TopLevelDestination.fromRoute(WidgetDeepLink.routeFor("settings")))
        assertEquals(TopLevelDestination.Home, TopLevelDestination.fromRoute(WidgetDeepLink.routeFor("home")))
        assertNull(TopLevelDestination.fromRoute(WidgetDeepLink.routeFor("willpower")))
        assertNull(TopLevelDestination.fromRoute(WidgetDeepLink.appDetailRoute("com.instagram.android")))
    }

    // -- Bar label sizing ---------------------------------------------------------------------

    /** A label is as wide as its characters times its size: enough to exercise the stepping. */
    private fun widthOf(text: String, size: androidx.compose.ui.unit.TextUnit) = text.length * size.value

    @Test
    fun `labels that fit keep the starting size`() {
        val size = fittingLabelSize(listOf("Home", "Apps"), 12.sp) { t, s -> widthOf(t, s) <= 100f }
        assertEquals(12.sp, size)
    }

    @Test
    fun `the longest label decides one size for all of them`() {
        val labels = listOf("Головна", "Застосунки", "Статистика", "Налаштування")
        val size = fittingLabelSize(labels, 12.sp) { t, s -> widthOf(t, s) <= 100f }
        assertTrue("shrunk below the start", size < 12.sp)
        assertTrue("every label fits at the chosen size", labels.all { widthOf(it, size) <= 100f })
        assertFalse(
            "and it is not smaller than it had to be",
            labels.all { widthOf(it, size * (1 / 0.95f)) <= 100f }
        )
    }

    @Test
    fun `a label that can never fit stops at the floor instead of looping`() {
        val size = fittingLabelSize(listOf("x"), 12.sp) { _, _ -> false }
        assertTrue(size <= 8.sp && size > 7.sp)
    }
}
