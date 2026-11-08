package com.astraedus.nudge.domain.lock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the pure Strict Mode OS-escape guard matcher.
 *
 * The window-text fixtures below are the REAL, concatenated visible text harvested from a Pixel 3
 * (Android 12, AOSP/Pixel Settings) on 2026-06-24 via `astra-adb dump`, app label
 * "Nudge - App Blocker". Keeping them real is the whole point: the matcher is only as good as the
 * actual node signatures it keys on.
 *
 * Safety bias under test: the matcher must guard the two escape routes, must NOT trap the user on
 * the accessibility LIST page (which also shows our label) or on unrelated Settings pages, and must
 * fully disengage when Strict Mode is off or a grace window is active.
 */
class StrictModeEscapeGuardTest {

    private val label = "Nudge - App Blocker"
    private val settingsPkg = "com.android.settings"

    /** Real text from the Nudge Accessibility detail page (where the service toggle lives). */
    private val accessibilityDetailText = """
        Use Nudge - App Blocker
        Nudge - App Blocker shortcut
        Nudge monitors which app is in the foreground to apply your blocking rules. No data leaves your device.
        Options
        Off
    """.trimIndent()

    /** Real text from the Nudge App Info page (Force stop / Uninstall). */
    private val appInfoText = """
        Nudge - App Blocker
        Open
        Uninstall
        Force stop
        Notifications
        Permissions
        Storage and cache
        Screen time
    """.trimIndent()

    /** Real text from the Accessibility LIST page — shows our label but must NOT be guarded. */
    private val accessibilityListText = """
        Downloaded apps
        Nudge - App Blocker
        Off
        TalkBack
        Screen reader
        Text and display
        Display
        Extra dim
    """.trimIndent()

    // ── the required matrix ──

    @Test
    fun `settings + a11y detail + strict on + no grace -- guard`() {
        assertTrue(
            StrictModeEscapeGuard.shouldGuardSettingsScreen(
                foregroundPkg = settingsPkg,
                windowText = accessibilityDetailText,
                appLabel = label,
                strictEnabled = true,
                withinGrace = false
            )
        )
    }

    @Test
    fun `same a11y detail but strict OFF -- no guard`() {
        assertFalse(
            StrictModeEscapeGuard.shouldGuardSettingsScreen(
                foregroundPkg = settingsPkg,
                windowText = accessibilityDetailText,
                appLabel = label,
                strictEnabled = false,
                withinGrace = false
            )
        )
    }

    @Test
    fun `same a11y detail but within grace -- no guard`() {
        assertFalse(
            StrictModeEscapeGuard.shouldGuardSettingsScreen(
                foregroundPkg = settingsPkg,
                windowText = accessibilityDetailText,
                appLabel = label,
                strictEnabled = true,
                withinGrace = true
            )
        )
    }

    @Test
    fun `non-settings package -- no guard`() {
        // Even if the text somehow contained our label + signals, a non-settings host is ignored.
        assertFalse(
            StrictModeEscapeGuard.shouldGuardSettingsScreen(
                foregroundPkg = "com.instagram.android",
                windowText = accessibilityDetailText,
                appLabel = label,
                strictEnabled = true,
                withinGrace = false
            )
        )
    }

    @Test
    fun `settings page without our app signals -- no guard`() {
        // Generic Settings home / unrelated page: no app label, no escape signals.
        val unrelated = """
            Network & internet
            Connected devices
            Apps
            Notifications
            Battery
            Storage
        """.trimIndent()
        assertFalse(
            StrictModeEscapeGuard.shouldGuardSettingsScreen(
                foregroundPkg = settingsPkg,
                windowText = unrelated,
                appLabel = label,
                strictEnabled = true,
                withinGrace = false
            )
        )
    }

    @Test
    fun `app info with uninstall and force stop -- guard`() {
        assertTrue(
            StrictModeEscapeGuard.shouldGuardSettingsScreen(
                foregroundPkg = settingsPkg,
                windowText = appInfoText,
                appLabel = label,
                strictEnabled = true,
                withinGrace = false
            )
        )
    }

    // ── false-positive guards (safety bias: fewer false positives) ──

    @Test
    fun `accessibility LIST page is NOT guarded even though it shows our label`() {
        // Critical: the list of all accessibility services contains "Nudge - App Blocker" but no
        // "shortcut"/"Use <label>"/force-stop/uninstall signal. Guarding here would trap the user.
        assertFalse(
            StrictModeEscapeGuard.shouldGuardSettingsScreen(
                foregroundPkg = settingsPkg,
                windowText = accessibilityListText,
                appLabel = label,
                strictEnabled = true,
                withinGrace = false
            )
        )
    }

    @Test
    fun `another app's app-info page (force stop or uninstall) without our label -- no guard`() {
        val otherAppInfo = """
            Some Other App
            Uninstall
            Force stop
            Notifications
        """.trimIndent()
        assertFalse(
            StrictModeEscapeGuard.shouldGuardSettingsScreen(
                foregroundPkg = settingsPkg,
                windowText = otherAppInfo,
                appLabel = label,
                strictEnabled = true,
                withinGrace = false
            )
        )
    }

    @Test
    fun `blank app label fails closed -- no guard even on the real detail page`() {
        assertFalse(
            StrictModeEscapeGuard.shouldGuardSettingsScreen(
                foregroundPkg = settingsPkg,
                windowText = accessibilityDetailText,
                appLabel = "",
                strictEnabled = true,
                withinGrace = false
            )
        )
    }

    @Test
    fun `matching is case-insensitive on both label and signals`() {
        val upper = accessibilityDetailText.uppercase()
        assertTrue(
            StrictModeEscapeGuard.shouldGuardSettingsScreen(
                foregroundPkg = settingsPkg,
                windowText = upper,
                appLabel = label.uppercase(),
                strictEnabled = true,
                withinGrace = false
            )
        )
    }

    @Test
    fun `tolerated OEM settings package is matched`() {
        assertTrue("com.miui.securitycenter" in StrictModeEscapeGuard.SETTINGS_PACKAGES)
        assertTrue(
            StrictModeEscapeGuard.shouldGuardSettingsScreen(
                foregroundPkg = "com.miui.securitycenter",
                windowText = appInfoText,
                appLabel = label,
                strictEnabled = true,
                withinGrace = false
            )
        )
    }

    // ── Ukrainian Settings (the label is the same in every language, see values-uk/strings.xml) ──
    //
    // Fixtures are built from the AOSP `values-uk` Settings strings and the common Pixel/Samsung
    // wording, NOT from a captured Ukrainian device dump: they pin the matcher's vocabulary and need
    // a device check on a uk phone (reported in the PR).

    /** The accessibility detail page of a Ukrainian phone: "Використовувати ...", "Ярлик ...". */
    private val ukAccessibilityDetailText = """
        Використовувати Nudge - App Blocker
        Ярлик Nudge - App Blocker
        Nudge відстежує, який застосунок відкрито на передньому плані, щоб застосовувати ваші правила блокування.
        Параметри
        Вимкнено
    """.trimIndent()

    /** A Ukrainian App Info page (Pixel wording). */
    private val ukAppInfoText = """
        Nudge - App Blocker
        Відкрити
        Видалити
        Примусово зупинити
        Сповіщення
        Дозволи
        Пам’ять і кеш
    """.trimIndent()

    /** The Ukrainian accessibility LIST: shows our label, no detail signal, must stay unguarded. */
    private val ukAccessibilityListText = """
        Завантажені застосунки
        Nudge - App Blocker
        Вимкнено
        TalkBack
        Зчитувач екрана
        Текст і дисплей
        Додатковий приглушений екран
    """.trimIndent()

    private fun guards(text: String, pkg: String = settingsPkg) =
        StrictModeEscapeGuard.shouldGuardSettingsScreen(
            foregroundPkg = pkg,
            windowText = text,
            appLabel = label,
            strictEnabled = true,
            withinGrace = false
        )

    @Test
    fun `ukrainian accessibility detail page -- guard`() {
        assertTrue(guards(ukAccessibilityDetailText))
    }

    @Test
    fun `ukrainian detail page is guarded by the use-label row alone, without the shortcut word`() {
        assertTrue(guards("Використовувати Nudge - App Blocker\nВимкнено"))
    }

    @Test
    fun `ukrainian app info with Примусово зупинити -- guard`() {
        assertTrue(guards(ukAppInfoText))
    }

    @Test
    fun `ukrainian app info on Samsung wording Примусова зупинка -- guard`() {
        assertTrue(
            guards(
                """
                Nudge - App Blocker
                Відкрити
                Примусова зупинка
                Сповіщення
                """.trimIndent(),
                pkg = "com.samsung.android.settings"
            )
        )
    }

    @Test
    fun `ukrainian uninstall alone with our label -- guard`() {
        assertTrue(guards("Nudge - App Blocker\nВидалити"))
    }

    @Test
    fun `ukrainian accessibility LIST page is NOT guarded`() {
        assertFalse(guards(ukAccessibilityListText))
    }

    @Test
    fun `ukrainian app info of another app without our label -- no guard`() {
        assertFalse(guards("Інший застосунок\nВидалити\nПримусово зупинити"))
    }

    @Test
    fun `ukrainian matching is case-insensitive`() {
        assertTrue(guards(ukAppInfoText.uppercase()))
    }

    @Test
    fun `ukrainian words on a non-settings package -- no guard`() {
        assertFalse(guards(ukAppInfoText, pkg = "com.instagram.android"))
    }

    @Test
    fun `a ukrainian phone still guards the english pages (any known language matches)`() {
        assertTrue(guards(accessibilityDetailText))
        assertTrue(guards(appInfoText))
    }

    @Test
    fun `ukrainian use-service toggle, stop and quick-enable wording -- guard`() {
        assertTrue(guards("Використовувати сервіс Nudge - App Blocker\nВимкнено"))
        assertTrue(guards("Nudge - App Blocker\nЗупинити"))
        assertTrue(guards("Nudge - App Blocker\nШвидке ввімкнення"))
    }

    // ── Unicode noise in the Settings text ──

    @Test
    fun `no-break and narrow no-break spaces inside a signal or the label do not hide the page`() {
        assertTrue(guards("Nudge - App Blocker\nПримусова\u00A0зупинка"))
        assertTrue(guards("Nudge - App Blocker\nПримусова\u202Fзупинка"))
        assertTrue(guards("Nudge - App Blocker\nForce\u00A0stop"))
        assertTrue(guards("Nudge\u00A0-\u00A0App\u00A0Blocker\nForce stop"))
        assertTrue(guards("Nudge\u202F-\u202FApp\u202FBlocker\nПримусова\u2007зупинка"))
        assertTrue(guards("Використовувати\u00A0Nudge - App Blocker\nВимкнено"))
    }

    @Test
    fun `a no-break space inside the app label from PackageManager still matches the plain label`() {
        assertTrue(
            StrictModeEscapeGuard.shouldGuardSettingsScreen(
                foregroundPkg = settingsPkg,
                windowText = appInfoText,
                appLabel = "Nudge\u00A0-\u00A0App Blocker",
                strictEnabled = true,
                withinGrace = false
            )
        )
    }

    @Test
    fun `runs of whitespace and zero-width characters collapse`() {
        assertTrue(guards("Nudge  -   App Blocker\nForce \t  stop"))
        assertTrue(guards("Nudge - App Blocker\nFor\u200Bce stop"))
    }

    @Test
    fun `a button word with stray no-break spaces around it is still the button`() {
        assertTrue(guards("Nudge - App Blocker\n\u00A0Видалити\u202F"))
    }

    @Test
    fun `typographic and plain apostrophes in the label are the same`() {
        val settingsLabel = "Nudge’s Blocker" // right single quote, as a Settings string may draw it
        fun guardsWith(label: String, text: String) = StrictModeEscapeGuard.shouldGuardSettingsScreen(
            foregroundPkg = settingsPkg,
            windowText = text,
            appLabel = label,
            strictEnabled = true,
            withinGrace = false
        )
        assertTrue(guardsWith("Nudge's Blocker", "$settingsLabel\nForce stop"))
        assertTrue(guardsWith("Nudge\u02BCs Blocker", "Nudge's Blocker\nForce stop"))
        assertTrue(guardsWith(settingsLabel, "Nudge's Blocker\nВидалити"))
    }

    // ── "Delete data" on a storage sub-page is not the uninstall button ──
    //
    // The bare uninstall / stop words (Видалити, Зупинити) count only when a
    // whole node says exactly that. The storage page also shows the app's label, and "Видалити дані"
    // only clears data, so guarding it would trap the user on a page that is not
    // an escape route. The English "Uninstall" and the force-stop phrases stay substring matches.

    @Test
    fun `storage sub-page with Видалити дані is not guarded`() {
        assertFalse(guards("Nudge - App Blocker\nВидалити дані\nОчистити кеш\nВикористано 12 МБ"))
    }

    @Test
    fun `the same storage page with the real buttons next to it is still guarded`() {
        assertTrue(guards("Nudge - App Blocker\nВидалити дані\nВидалити"))
    }
}
