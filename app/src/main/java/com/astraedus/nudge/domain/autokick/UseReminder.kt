package com.astraedus.nudge.domain.autokick

/**
 * One usage reminder ([#77](https://github.com/astraedus/nudge/issues/77)): which app, and for how
 * long. The words are in `service/NotificationText.kt` and `strings_service.xml` in res/values and res/values-uk.
 *
 * The voice is the overlay's (`NudgeMessages`): a friend tapping your shoulder, never a scold. It
 * states a fact the user asked to be told, then asks one open question, and it never says
 * "too long", "wasted" or "again". The exact copy is pinned by a JVM test.
 *
 * @param appLabel the user-visible app name ("Instagram"); falls back to the package name upstream.
 * @param minutes the reminder threshold the user configured, which is what they asked to hear back.
 *   It is the configured N rather than the measured elapsed time on purpose: the clock ticks every
 *   30s, so the measurement can read 20.4 minutes, and "20 min" is the number the user chose.
 */
data class UseReminder(val appLabel: String, val minutes: Int)
