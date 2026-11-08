package com.astraedus.nudge.domain.lock

import java.util.Locale

/**
 * Pure-Kotlin decision logic for Strict Mode "OS escape route" guarding (Phase 2).
 *
 * Phase 1 gates Nudge's own UI behind the typed unlock challenge. The obvious bypass that
 * remained is leaving the app entirely:
 *   1. Settings → Accessibility → Nudge → turn the service toggle OFF, or
 *   2. App Info / App details for Nudge → Force stop / Uninstall.
 *
 * While Strict Mode is on, when the user lands on one of those screens we intercept with the
 * SAME unlock challenge. This object decides *whether* to guard; it has no Android imports and
 * is fully unit-testable.
 *
 * ## Real-device signatures this matcher was tuned to
 * Captured via `astra-adb dump` on a Pixel 3 (Android 12, AOSP/Pixel Settings) on 2026-06-24,
 * app label "Nudge - App Blocker", package `dev.astraedus.nudge`:
 *
 * - **Accessibility detail page** — window `com.android.settings/.SubSettings`. Node text includes
 *   the toggle label "Use Nudge - App Blocker", "Nudge - App Blocker shortcut", and the service
 *   description. Discriminator: the app label co-occurs with the word "shortcut" OR the "Use <label>"
 *   toggle row. The Accessibility LIST page ALSO shows "Nudge - App Blocker" (and an "Accessibility"
 *   header), so we deliberately do NOT key on the bare word "accessibility" — that would trap the
 *   user on the list of all services. "shortcut" / "use <label>" are present only on the detail page.
 * - **App Info page** — window `com.android.settings/.applications.InstalledAppDetails`. Node text
 *   includes the app label "Nudge - App Blocker" plus action buttons "Force stop" and "Uninstall".
 *   Discriminator: app label co-occurs with "Force stop" or "Uninstall".
 *
 * ## Safety bias
 * This matcher is intentionally biased toward FEWER false positives — it is better to occasionally
 * miss a guard than to trap the user on an unrelated Settings page. Every positive requires BOTH a
 * settings package AND the app label AND a strong escape-route signal. Strict Mode OFF or an active
 * grace window short-circuits to "do not guard" immediately.
 */
object StrictModeEscapeGuard {

    /**
     * Settings packages we tolerate as the host of a protected screen. AOSP/Pixel first
     * (`com.android.settings`), plus the most common OEM settings packages so the guard still
     * bites on Samsung/Xiaomi/etc. Adding a package here only *enables* matching; the text
     * signals below still have to be present, so a broad list does not by itself cause false
     * positives.
     */
    val SETTINGS_PACKAGES: Set<String> = setOf(
        "com.android.settings",            // AOSP / Pixel
        "com.samsung.android.settings",    // some Samsung builds
        "com.miui.securitycenter",         // Xiaomi MIUI app-ops / app details
        "com.coloros.safecenter",          // Oppo/Realme ColorOS
        "com.oplus.settings",              // newer Oppo/OnePlus OxygenOS/ColorOS
        "com.oneplus.settings",            // OnePlus
        "com.vivo.settings",               // Vivo
        "com.transsion.settings"           // Tecno/Infinix
    )

    /**
     * Keywords that, alongside the app label, mark the Accessibility detail page for our service:
     * the shortcut row ("<label> shortcut"). We deliberately avoid the bare word
     * "accessibility" because it appears as a header on the Accessibility LIST page (which also
     * lists our label) and would cause a false positive there. Written in natural case and
     * normalised with [normalize] before use, so case, NBSP and apostrophe variants do not matter.
     *
     * ## Languages
     * The guard has to bite whatever language the PHONE is set to, because Settings draws these
     * words in the system language, not Nudge's. So every sentence lives here once per language
     * we know, and the matcher accepts ANY of them (no locale is consulted: a phone's language is
     * not a trustworthy read of what the Settings window shows, e.g. per-app language).
     *
     * - **English**: AOSP/Pixel `accessibility_shortcut_title` "...shortcut".
     * - **Ukrainian**: the same AOSP string in `values-uk` ("Ярлик ..."), and the shortcut title of
     *   the AOSP `Accessibility` page ("Швидке ввімкнення").
     *
     * NONE of the Ukrainian words was read off a device: they are the AOSP resource wording from
     * memory and need an a11y-capture on a uk phone (L6).
     */
    private val ACCESSIBILITY_DETAIL_SIGNALS = listOf(
        "shortcut",            // en
        "ярлик",               // uk: "Ярлик служби", "Ярлик Nudge"
        "швидке ввімкнення"    // uk: "Швидке ввімкнення"
    ).map(::normalize)

    /**
     * The "<prefix> <label>" master-switch row of the accessibility detail page, per language.
     * The prefixes are followed by the app label, which makes them specific (the list page shows
     * the label alone).
     */
    private val ACCESSIBILITY_SWITCH_PREFIXES = listOf(
        "Use ",                       // en: "Use Nudge - App Blocker"
        "Використовувати ",           // uk: "Використовувати Nudge - App Blocker"
        "Використовувати сервіс "     // uk: "Використовувати сервіс Nudge - App Blocker"
    ).map { normalize(it) + " " } // normalize trims, the separating space is part of the prefix

    /**
     * Phrases that, alongside the app label, mark the App Info page (Force stop / Uninstall) when
     * they appear ANYWHERE in the window text. They are specific enough to be safe as substrings.
     *
     * - en: "Force stop", "Uninstall" (AOSP `force_stop`, `uninstall_text`).
     * - uk: "Примусово зупинити" (AOSP/Pixel), "Примусова зупинка" (Samsung One UI).
     */
    private val APP_INFO_SIGNALS = listOf(
        "Force stop", "Uninstall",
        "Примусово зупинити", "Примусова зупинка"
    ).map(::normalize)

    /**
     * The bare button words of the App Info page. They are common words that also START longer
     * labels elsewhere ("Видалити дані" on the storage sub-page, which also
     * shows the app's label), so they count only when a whole node of the window says exactly this
     * (the button's own text), never as a substring of a longer line.
     *
     * - uk: "Видалити" (AOSP `uninstall_text`), "Зупинити" (AOSP `force_stop`).
     *
     * Same device-capture caveat as above. A miss here costs nothing the force-stop phrase or the
     * accessibility page does not also catch, and a wrongful hit would trap the user on a page
     * that only clears data.
     */
    private val APP_INFO_BUTTONS = listOf(
        "Видалити", "Зупинити"
    ).map(::normalize)

    /**
     * The one text form the matcher compares in, applied to the screen text, the label and every
     * keyword list alike:
     *  - lowercase with [Locale.ROOT] (never the device locale: Turkish would turn "I" into a
     *    dotless "ı");
     *  - every Unicode space (NBSP U+00A0, figure space U+2007, narrow NBSP U+202F, thin, ideographic
     *    ...) and every control or tab becomes a plain space, and runs of spaces collapse to one, so
     *    "Примусова\u00A0зупинка" equals "примусова зупинка";
     *  - invisible format characters (zero-width space/joiner, word joiner, soft hyphen, BOM) go;
     *  - the apostrophe family (U+2019 right quote, U+02BC modifier letter, U+2018, U+0060, U+00B4,
     *    U+2032) becomes ASCII `'`, so "Пам’ять" and "Пам'ять" agree.
     * Line breaks are kept (as `\n`) because the service separates window nodes with them, and the
     * button check below needs node boundaries.
     */
    internal fun normalize(text: String): String {
        val sb = StringBuilder(text.length)
        var lastWasSpace = true // also trims the start of every line
        for (ch in text.lowercase(Locale.ROOT)) {
            when {
                ch == '\n' || ch == '\r' || ch == '\u2028' || ch == '\u2029' || ch == '\u0085' -> {
                    if (sb.isNotEmpty() && sb.last() == ' ') sb.setLength(sb.length - 1)
                    if (sb.isEmpty() || sb.last() != '\n') sb.append('\n')
                    lastWasSpace = true
                }
                ch in INVISIBLE -> Unit
                ch in APOSTROPHES -> {
                    sb.append('\'')
                    lastWasSpace = false
                }
                Character.isSpaceChar(ch) || Character.isWhitespace(ch) || Character.isISOControl(ch) -> {
                    if (!lastWasSpace) sb.append(' ')
                    lastWasSpace = true
                }
                else -> {
                    sb.append(ch)
                    lastWasSpace = false
                }
            }
        }
        return sb.toString().trim()
    }

    private const val INVISIBLE = "\u200B\u200C\u200D\u2060\uFEFF\u00AD"
    private const val APOSTROPHES = "\u2019\u02BC\u2018\u0060\u00B4\u2032"

    /**
     * Decide whether to launch the unlock challenge over the current foreground screen.
     *
     * @param foregroundPkg the package owning the foreground window.
     * @param windowText concatenated, user-visible text harvested from the foreground window's
     *   node tree (text + content descriptions). Case-insensitive matching is applied internally.
     * @param appLabel our own app's user-visible label (read at runtime from PackageManager so we
     *   never hardcode "Nudge - App Blocker"); e.g. "Nudge - App Blocker".
     * @param strictEnabled whether Strict Mode is currently on. OFF → never guard.
     * @param withinGrace whether we are inside a post-unlock grace window. In grace → never guard
     *   (so a user who just solved the challenge can actually complete the disable they committed to).
     * @return true if the protected-screen challenge overlay should be shown.
     */
    fun shouldGuardSettingsScreen(
        foregroundPkg: String,
        windowText: String,
        appLabel: String,
        strictEnabled: Boolean,
        withinGrace: Boolean
    ): Boolean {
        // Cheap short-circuits first — these dominate the common case and keep us off the hot path.
        if (!strictEnabled) return false
        if (withinGrace) return false
        if (foregroundPkg !in SETTINGS_PACKAGES) return false

        val haystack = normalize(windowText)
        val needleLabel = normalize(appLabel)
        // Without a non-blank label we cannot anchor the match → fail closed (do not guard).
        if (needleLabel.isEmpty()) return false
        if (!haystack.contains(needleLabel)) return false

        // Accessibility detail: "<label> shortcut" or the "Use <label>" toggle row — both present
        // only on the detail page, not the list of all services.
        val isAccessibilityDetail = ACCESSIBILITY_DETAIL_SIGNALS.any { haystack.contains(it) } ||
            ACCESSIBILITY_SWITCH_PREFIXES.any { haystack.contains(it + needleLabel) }
        // App Info: a force-stop / uninstall phrase anywhere, or a bare button word that is a whole
        // node of its own (so "Видалити дані" on a storage page is not the uninstall button).
        val isAppInfo = APP_INFO_SIGNALS.any { haystack.contains(it) } ||
            haystack.lineSequence().any { it in APP_INFO_BUTTONS }
        return isAccessibilityDetail || isAppInfo
    }
}
