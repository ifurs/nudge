package com.astraedus.nudge.service

import com.astraedus.nudge.R
import com.astraedus.nudge.domain.autokick.UseReminder
import com.astraedus.nudge.domain.bounce.BounceAlert
import com.astraedus.nudge.domain.text.UiText
import com.astraedus.nudge.ui.text.DurationText

/**
 * The words of the two opt-in notifications, as [UiText]: the domain classes carry only the numbers
 * ([BounceAlert], [UseReminder]) and this is where they become sentences, in the user's language.
 */

fun BounceAlert.titleText(): UiText = UiText.of(R.string.service_bounce_title)

/** "You've bounced through 6 apps in 4 minutes. Wanna take a break?" */
fun BounceAlert.bodyText(): UiText {
    val things = UiText.plural(
        if (unit == BounceAlert.Unit.SITES) R.plurals.service_bounce_sites else R.plurals.service_bounce_apps,
        count
    )
    return UiText.of(R.string.service_bounce_body, things, UiText.plural(R.plurals.service_bounce_minutes, minutes))
}

/** "You've been on Instagram for 20 min" */
fun UseReminder.titleText(): UiText =
    UiText.of(R.string.service_use_reminder_title, appLabel, DurationText.spoken(minutes))

fun UseReminder.bodyText(): UiText = UiText.of(R.string.service_use_reminder_body)
