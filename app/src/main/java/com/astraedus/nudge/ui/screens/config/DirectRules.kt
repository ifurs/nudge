package com.astraedus.nudge.ui.screens.config

import com.astraedus.nudge.data.db.entity.BlockRule

/**
 * Whether a direct app rule is a scheduled one (spec 3.5): a days-and-times window or a first-unlock
 * window.
 *
 * `scheduleDays != null` stays in the test on purpose: a days-only rule (days set, no times) used to
 * count as the scheduled override, and treating it as the default would have a save rewrite it as
 * always-on. A first-unlock rule always carries days too, and an imported one may carry none, which
 * the other checks catch.
 *
 * One definition for the two places that sort an app's rules into "always" and "scheduled": the
 * app config screen and the Home "Block short videos" card.
 */
internal fun BlockRule.isScheduledRule(): Boolean =
    firstUnlockMinutes != null ||
        scheduleStartMinute != null ||
        scheduleEndMinute != null ||
        scheduleDays != null
