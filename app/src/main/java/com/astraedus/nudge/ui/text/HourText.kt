package com.astraedus.nudge.ui.text

import com.astraedus.nudge.R
import com.astraedus.nudge.domain.text.UiText

/**
 * An hour of the day for chart axes and callouts.
 *
 * 0 -> "12am", 13 -> "1pm" (English); "0:00", "13:00" (Ukrainian, a 24-hour clock). Both the
 * 12-hour and the 24-hour value are passed, and each language's string uses the one it reads.
 * Out-of-range hours degrade to the raw number.
 */
object HourText {
    fun of(hour: Int): UiText = when {
        hour !in 0..23 -> UiText.Raw(hour.toString())
        hour == 0 -> UiText.of(R.string.stats_hour_am, 12, hour)
        hour < 12 -> UiText.of(R.string.stats_hour_am, hour, hour)
        hour == 12 -> UiText.of(R.string.stats_hour_pm, 12, hour)
        else -> UiText.of(R.string.stats_hour_pm, hour - 12, hour)
    }
}
