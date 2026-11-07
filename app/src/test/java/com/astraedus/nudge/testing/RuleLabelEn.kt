package com.astraedus.nudge.testing

import com.astraedus.nudge.domain.model.RuleLabel
import com.astraedus.nudge.ui.overlay.toUiText

/**
 * A [RuleLabel] as the block screen's "Rule: ..." line reads it in English: "Social (scheduled)",
 * "Reels - Delay", "Web - Hard Block". The engine tests assert the label as DATA where the data is
 * the point and through this where the words are.
 */
fun RuleLabel?.en(): String? = this?.toUiText()?.en()

/** [en], for a label that must exist. */
fun RuleLabel.enText(): String = toUiText().en()
