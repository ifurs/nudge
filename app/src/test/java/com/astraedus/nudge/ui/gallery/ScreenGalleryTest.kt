package com.astraedus.nudge.ui.gallery

import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.Column
import androidx.core.app.ActivityOptionsCompat
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.NightMode
import com.android.resources.ScreenOrientation
import com.astraedus.nudge.ui.components.rule.ScheduleKind
import com.astraedus.nudge.ui.components.rule.WhenSection
import com.astraedus.nudge.ui.overlay.HardBlockContent
import com.astraedus.nudge.ui.redirect.RedirectAppBubble
import com.astraedus.nudge.ui.screens.apps.AppListContent
import com.astraedus.nudge.ui.navigation.NudgeBottomBar
import com.astraedus.nudge.ui.navigation.NudgeBottomBarScaffold
import com.astraedus.nudge.ui.navigation.Screen
import com.astraedus.nudge.ui.navigation.TopLevelDestination
import com.astraedus.nudge.ui.screens.config.UnifiedAppConfigActions
import com.astraedus.nudge.ui.screens.config.UnifiedAppConfigContent
import com.astraedus.nudge.ui.screens.home.HomeContent
import com.astraedus.nudge.ui.screens.home.ShortVideoState
import com.astraedus.nudge.ui.theme.NudgeTheme
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

/**
 * Golden images of curated screen states (Paparazzi, JVM-only via layoutlib; no device, no
 * Robolectric). How to add a screen, record and review a diff: `docs/screenshot-tests.md`.
 *
 * Every screen renders from [GalleryFixtures], never from real user data, through the same
 * stateless `XContent(state, callbacks)` entry point its production screen calls.
 *
 * Pixel 3 geometry (393dp wide) because that is the bench device every manual QA run uses, and a
 * narrow screen is where clipped switches and overflowing labels show first.
 *
 * `dynamicColor = false`: the device's wallpaper palette would replace the brand colours on
 * Android 12+, and a golden must not depend on whatever wallpaper layoutlib assumes.
 */
class ScreenGalleryTest {

    /**
     * `maxPercentDifference = 0.0`: any changed pixel fails. Paparazzi's default lets 0.1% through,
     * and #154's lost 12dp switch inset changes only 0.22% of the Home frame, so a 4dp slip would
     * pass unseen. layoutlib renders in software with its own bundled fonts, and the CI goldens
     * were byte-identical to a laptop render, so zero tolerance does not make the gate flaky.
     */
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_3, maxPercentDifference = 0.0)

    /** A 360dp-wide phone at 1.3x font scale, where the reported folding first showed. */
    private val phone360 = DeviceConfig.PIXEL_3.copy(
        screenWidth = 1080,
        screenHeight = 2400,
        xdpi = 480,
        ydpi = 480,
        orientation = ScreenOrientation.PORTRAIT,
        density = Density.XXHIGH,
        fontScale = 1.3f
    )

    @Test
    fun home_light() = snapshot(dark = false) { tab(Screen.Home.route) { home() } }

    @Test
    fun home_dark() = snapshot(dark = true) { tab(Screen.Home.route) { home() } }

    /** The card with the "Nudge can't see Reels yet" notice and its button. */
    @Test
    fun home_accessibilityOff_light() = snapshot(dark = false) {
        tab(Screen.Home.route) { home(GalleryFixtures.shortVideos(accessibilityWorking = false)) }
    }

    @Test
    fun home_uk_light() = snapshot(dark = false, deviceConfig = ukrainian()) {
        tab(Screen.Home.route) { home() }
    }

    @Test
    fun home_uk_dark() = snapshot(dark = true, deviceConfig = ukrainian()) {
        tab(Screen.Home.route) { home() }
    }

    /** Ukrainian at 360dp and 1.3x font: the longest labels, in the narrowest layout. */
    @Test
    fun home_uk_360dp() = snapshot(dark = false, deviceConfig = ukrainian(phone360)) {
        tab(Screen.Home.route) { home() }
    }

    @Test
    fun home_360dp() = snapshot(dark = false, deviceConfig = phone360) {
        tab(Screen.Home.route) { home() }
    }

    /**
     * The whole dashboard top to bottom (a viewport tall enough that nothing scrolls out of the
     * frame), so every card, tile and label is in a golden rather than only the first screenful.
     */
    @Test
    fun home_full_light() = snapshot(dark = false, deviceConfig = tall()) {
        tab(Screen.Home.route) { home() }
    }

    @Test
    fun home_uk_full_light() = snapshot(dark = false, deviceConfig = ukrainian(tall())) {
        tab(Screen.Home.route) { home() }
    }

    @Test
    fun home_uk_full_360dp() = snapshot(dark = false, deviceConfig = ukrainian(tall(phone360))) {
        tab(Screen.Home.route) { home() }
    }

    /** The bar alone, in the longest Ukrainian labels and the narrowest layout. */
    @Test
    fun bottomBar_uk_360dp() = snapshot(dark = false, deviceConfig = ukrainian(phone360)) {
        Column {
            NudgeBottomBar(selected = TopLevelDestination.Apps, onSelect = {})
            NudgeBottomBar(selected = TopLevelDestination.Settings, onSelect = {})
        }
    }

    @Test
    fun manageApps_light() = snapshot(dark = false) { tab(Screen.Apps.route) { manageApps() } }

    @Test
    fun manageApps_uk() = snapshot(dark = false, deviceConfig = ukrainian()) {
        tab(Screen.Apps.route) { manageApps() }
    }

    @Test
    fun appConfig_instagram_light() = snapshot(dark = false) { appConfig() }

    @Test
    fun appConfig_instagram_uk() = snapshot(dark = false, deviceConfig = ukrainian()) { appConfig() }

    @Test
    fun appConfig_instagram_uk_360dp() = snapshot(
        dark = false,
        deviceConfig = ukrainian(phone360)
    ) { appConfig() }

    @Test
    fun blockOverlay_hardBlock_light() = snapshot(dark = false) {
        HardBlockContent(
            packageName = GalleryFixtures.BLOCKED_PACKAGE,
            appLabel = GalleryFixtures.BLOCKED_LABEL,
            onGoBack = {},
            ruleName = "Evenings",
            untilText = "Until 9:00 PM",
            // One entry, so the screen's random pick is deterministic. The text is a real
            // shipped message (NudgeMessages.hardBlockMessages), not one written for the test.
            messagePool = listOf(GalleryFixtures.hardBlockMessage()),
            canUseEmergencyPass = true,
            redirect = { RedirectAppBubble(target = null, onLaunch = {}, onEdit = {}) }
        )
    }

    @Test
    fun whenSection_firstUnlock_360dp() = snapshot(
        dark = false,
        deviceConfig = phone360
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            WhenSection(
                kind = ScheduleKind.FIRST_UNLOCK,
                onKindChange = {},
                days = (1..7).toSet(),
                onDaysChange = {},
                startHour = 9,
                startMinute = 0,
                onStartTimeChange = { _, _ -> },
                endHour = 17,
                endMinute = 0,
                onEndTimeChange = { _, _ -> },
                firstUnlockMinutes = 30,
                onFirstUnlockMinutesChange = {},
                dayStartMinute = 4 * 60,
                onDayStartClick = {},
                startsAtNextUnlock = true
            )
        }
    }

    /** A top-level screen inside the shell the app puts around it: the bottom bar on [route]. */
    @Composable
    private fun tab(route: String, content: @Composable () -> Unit) {
        NudgeBottomBarScaffold(currentRoute = route, onSelect = {}, content = content)
    }

    @Composable
    private fun home(shortVideos: ShortVideoState = GalleryFixtures.shortVideos()) {
        HomeContent(
            state = GalleryFixtures.home(),
            nukeSummary = GalleryFixtures.nukeSummary(),
            lockCard = null,
            shortVideos = shortVideos,
            onToggleGlobalEnabled = {},
            onToggleShortVideo = { _, _ -> },
            onShortVideoMoreOptions = {},
            onEnableAccessibility = {},
            onNavigateToApps = {},
            onNavigateToStats = {},
            onNavigateToWillpower = {},
            onNavigateToInterventions = {},
            onNavigateToAppDetail = {},
            onNavigateToNuke = {},
            onOpenAllAppsRule = {}
        )
    }

    @Composable
    private fun manageApps() {
        AppListContent(
            state = GalleryFixtures.manageApps(),
            onSearchQueryChange = {},
            onNavigateToAppConfig = {},
            onOpenGroup = {},
            onOpenAllAppsRule = {},
            onToggleAllApps = {},
            onToggleGroup = {},
            onToggleApp = {},
            onBlockSeveral = {}
        )
    }

    @Composable
    private fun appConfig() {
        // The usage-reminder row asks the notification service whether a reminder could arrive;
        // layoutlib has no such service, so the screen gets a context that answers "yes".
        val context = LocalContext.current
        val notificationsAllowed = remember(context) { NotificationsAllowedContext(context) }
        CompositionLocalProvider(
            LocalContext provides notificationsAllowed,
            LocalActivityResultRegistryOwner provides NoActivityResults
        ) {
            UnifiedAppConfigContent(
                state = GalleryFixtures.appConfig(),
                coveredBy = emptyList(),
                coveredByAllApps = emptyList(),
                actions = object : UnifiedAppConfigActions {},
                onNavigateBack = {},
                onOpenGroup = {},
                onOpenAllAppsRule = {}
            )
        }
    }

    /** The same row registers a permission launcher; nothing in a golden ever launches it. */
    private object NoActivityResults : ActivityResultRegistryOwner {
        override val activityResultRegistry = object : ActivityResultRegistry() {
            override fun <I : Any?, O : Any?> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?
            ) = Unit
        }
    }

    /** A context whose notification service says notifications are on, for the one row that asks. */
    private class NotificationsAllowedContext(base: Context) : ContextWrapper(base) {
        private val manager = mockk<NotificationManager>(relaxed = true).also {
            every { it.areNotificationsEnabled() } returns true
        }

        override fun getSystemService(name: String): Any? =
            if (name == Context.NOTIFICATION_SERVICE) manager else super.getSystemService(name)

        override fun <T : Any?> getSystemService(serviceClass: Class<T>): T? =
            if (serviceClass == NotificationManager::class.java) {
                serviceClass.cast(manager)
            } else {
                super.getSystemService(serviceClass)
            }
    }

    /** [base] with a viewport about twice as tall, for the full-page goldens. */
    private fun tall(base: DeviceConfig = DeviceConfig.PIXEL_3): DeviceConfig =
        base.copy(screenHeight = base.screenHeight * 2)

    private fun ukrainian(base: DeviceConfig = DeviceConfig.PIXEL_3): DeviceConfig = base.copy(locale = "uk")

    private fun snapshot(
        dark: Boolean,
        deviceConfig: DeviceConfig = DeviceConfig.PIXEL_3,
        content: @Composable () -> Unit
    ) {
        paparazzi.unsafeUpdateConfig(
            deviceConfig = if (dark) {
                deviceConfig.copy(nightMode = NightMode.NIGHT)
            } else {
                deviceConfig
            }
        )
        paparazzi.snapshot {
            NudgeTheme(darkTheme = dark, dynamicColor = false) { content() }
        }
    }
}
