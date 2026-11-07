package com.astraedus.nudge.ui.overlay

import com.astraedus.nudge.R
import com.astraedus.nudge.domain.model.BlockMode
import com.astraedus.nudge.domain.model.RuleLabel
import com.astraedus.nudge.domain.text.UiText
import com.astraedus.nudge.ui.components.blockModeLabelRes

/**
 * Words a [RuleLabel] for the block screen's "Rule: ..." line. The engine says WHAT the label is
 * about; this says it in the user's language. English readings are what the engine always built:
 * "Reels - Delay", "Social (scheduled)", "Hard Block (30 min/day)", "Web - Hard Block".
 */
internal fun RuleLabel.toUiText(): UiText {
    val parts = buildList {
        if (features.isNotEmpty()) {
            add(UiText.Raw(features.joinToString(", ") { it.replaceFirstChar { c -> c.uppercaseChar() } }))
        }
        add(subject.toUiText())
    }
    var label: UiText = UiText.Joined(parts, " - ")

    val qualifiers = listOfNotNull(
        if (scheduled) UiText.of(R.string.overlay_rule_scheduled) else null,
        dailyLimitMinutes?.let { UiText.of(R.string.overlay_rule_per_day, it) }
    )
    if (qualifiers.isNotEmpty()) {
        label = UiText.of(R.string.overlay_rule_with_qualifiers, label, UiText.Joined(qualifiers, ", "))
    }
    if (limitReached) label = UiText.of(R.string.overlay_rule_limit_reached, label)
    return label
}

private fun RuleLabel.Subject.toUiText(): UiText = when (this) {
    is RuleLabel.Subject.Mode -> when (mode) {
        BlockMode.NONE ->
            UiText.of(if (capsUsage) R.string.overlay_rule_daily_limit else R.string.overlay_rule_no_block)
        else -> UiText.of(blockModeLabelRes(mode))
    }
    is RuleLabel.Subject.Group -> name?.let { UiText.Raw(it) } ?: UiText.of(R.string.overlay_rule_group)
    RuleLabel.Subject.AllApps -> UiText.of(R.string.components_summary_all_apps)
    is RuleLabel.Subject.Web -> UiText.of(R.string.overlay_rule_web, UiText.of(blockModeLabelRes(mode)))
    RuleLabel.Subject.ContentFilter -> UiText.of(R.string.overlay_rule_content_filter)
    RuleLabel.Subject.Nuke -> UiText.of(R.string.overlay_rule_nuke)
    is RuleLabel.Subject.Named -> UiText.Raw(text)
}
