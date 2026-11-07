package com.astraedus.nudge.ui.overlay

import android.content.res.Resources
import androidx.annotation.ArrayRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import com.astraedus.nudge.R

/**
 * The built-in message pools, as string arrays in `strings_overlay.xml` in res/values and res/values-uk so each language
 * ships its own set (`docs/i18n.md`). The user's own lines (Settings -> Messages) are data, not
 * copy, and stay as typed.
 */
object NudgeMessages {
    @ArrayRes val DELAY_TITLES: Int = R.array.overlay_delay_titles
    @ArrayRes val DELAY_SUBTITLES: Int = R.array.overlay_delay_subtitles
    @ArrayRes val HARD_BLOCK_MESSAGES: Int = R.array.overlay_hard_block_messages

    /** For composables, which re-read on a locale change. */
    @Composable fun delayTitles(): List<String> = stringArrayResource(DELAY_TITLES).toList()
    @Composable fun delaySubtitles(): List<String> = stringArrayResource(DELAY_SUBTITLES).toList()
    @Composable fun hardBlockMessages(): List<String> = stringArrayResource(HARD_BLOCK_MESSAGES).toList()

    /** For the activity, which resolves the pools before it composes. */
    fun delayTitles(resources: Resources): List<String> = resources.getStringArray(DELAY_TITLES).toList()
    fun delaySubtitles(resources: Resources): List<String> = resources.getStringArray(DELAY_SUBTITLES).toList()
    fun hardBlockMessages(resources: Resources): List<String> =
        resources.getStringArray(HARD_BLOCK_MESSAGES).toList()

    /**
     * Resolves the pool of messages to display, given the user's custom multiline text
     * and the built-in default list.
     *
     * The custom text is one message per line; lines are trimmed and blanks dropped.
     * If the result is empty (null/blank/whitespace-only custom text), falls back to
     * [default]. Pure (no Android imports) so it is JVM-unit-testable.
     */
    fun resolvePool(customRaw: String?, default: List<String>): List<String> {
        val lines = customRaw?.split("\n")?.map(String::trim)?.filter(String::isNotBlank) ?: emptyList()
        return lines.ifEmpty { default }
    }
}
