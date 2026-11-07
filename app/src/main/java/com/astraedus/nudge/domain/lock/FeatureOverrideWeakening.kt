package com.astraedus.nudge.domain.lock

import com.astraedus.nudge.data.db.entity.BlockRule

/**
 * Whether saving an app's in-app feature overrides (Reels, Shorts, the TikTok feed) weakens them.
 *
 * App config rewrites an app's rows wholesale, so a feature override that the new state no longer
 * has (Hard Block set back to Inherit writes no row at all) is a row that is DELETED. That is
 * exactly as weakening as switching the row off, so it is judged the same way: against
 * [RuleWeakening.isWeakening], with the missing row read as the old one turned off. A row that is
 * still there is judged field by field by [RuleWeakening] itself. A feature with no row before,
 * and any extra row, only adds protection and never counts.
 *
 * Pure Kotlin, keyed by feature (`inAppFeatures`), the same keys the editor uses.
 */
object FeatureOverrideWeakening {

    fun isWeakened(old: Map<String, BlockRule>, new: Map<String, BlockRule>): Boolean =
        old.any { (feature, oldRule) ->
            RuleWeakening.isWeakening(oldRule, new[feature] ?: oldRule.copy(enabled = false))
        }
}
