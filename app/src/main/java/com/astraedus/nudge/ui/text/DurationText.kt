package com.astraedus.nudge.ui.text

import com.astraedus.nudge.R
import com.astraedus.nudge.domain.text.UiText

/**
 * Durations as [UiText]: the unit abbreviations ("h", "m", "s") are copy, so they come from
 * resources ("год", "хв", "с" in Ukrainian) instead of being glued onto numbers in a ViewModel.
 * Pure: the same arithmetic the English-only `TimeTracker.formatDuration` did.
 */
object DurationText {

    /** "2h 5m", "45m", "1m 30s", "30s", "0s": the stats screens' standard duration. */
    fun of(ms: Long): UiText {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return when {
            hours > 0 -> UiText.of(R.string.components_duration_short_hours_minutes, hours, minutes)
            minutes > 0 ->
                if (seconds > 0) {
                    UiText.of(R.string.components_duration_short_minutes_seconds, minutes, seconds)
                } else {
                    UiText.of(R.string.components_duration_short_minutes, minutes)
                }
            else -> UiText.of(R.string.components_duration_short_seconds, seconds)
        }
    }

    /**
     * A day's screen-time total, worded the same on every day-scoped screen: a day with SOME use
     * under a minute is "< 1m", a day with none is "0s" (not "under a minute").
     */
    fun dayTotal(ms: Long): UiText =
        if (ms in 1L until 60_000L) UiText.of(R.string.components_duration_under_minute) else of(ms)

    /** "2h 5m", "45m", "< 1m": bar labels, where seconds are noise. */
    fun short(ms: Long): UiText {
        val h = ms / 3_600_000
        val m = (ms % 3_600_000) / 60_000
        return when {
            h > 0L -> UiText.of(R.string.components_duration_short_hours_minutes, h, m)
            m > 0L -> UiText.of(R.string.components_duration_short_minutes, m)
            else -> UiText.of(R.string.components_duration_under_minute)
        }
    }

    /** "2h 5m", "45m", "30s", "0s": the on-screen counter's compact form. */
    fun compact(ms: Long): UiText {
        if (ms <= 0) return UiText.of(R.string.components_duration_short_seconds, 0)
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return when {
            hours > 0 -> UiText.of(R.string.components_duration_short_hours_minutes, hours, minutes)
            minutes > 0 -> UiText.of(R.string.components_duration_short_minutes, minutes)
            else -> UiText.of(R.string.components_duration_short_seconds, totalSeconds)
        }
    }

    /** "20 min", "1 hr", "1 hr 30 min": a notification's spoken duration. */
    fun spoken(minutes: Int): UiText {
        val safe = minutes.coerceAtLeast(1)
        val hours = safe / 60
        val rest = safe % 60
        return when {
            hours == 0 -> UiText.of(R.string.components_duration_spoken_minutes, rest)
            rest == 0 -> UiText.of(R.string.components_duration_spoken_hours, hours)
            else -> UiText.of(R.string.components_duration_spoken_hours_minutes, hours, rest)
        }
    }
}
