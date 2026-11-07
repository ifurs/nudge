package com.astraedus.nudge.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.astraedus.nudge.R
import com.astraedus.nudge.domain.model.BlockMode

/**
 * The words the app puts on a [BlockMode], in ONE place.
 *
 * Two screens offer the same mode picker — the rule editor and the unified app config — and they
 * each carried their own `when` over every mode, so the same enum could be called two different
 * things depending on which screen the user reached it from. Worse, a new mode had to be spelled
 * out twice before it was fully visible, and the compiler only told you about the copy you
 * remembered to look at.
 *
 * Each function is an exhaustive `when` with no `else`, so adding a mode to [BlockMode] does not
 * compile until it has been given a name and a sentence. The `...Res` form returns the resource id
 * (pure, so a JVM test can resolve it); the composable form of the same name reads it.
 */
@StringRes
internal fun blockModeLabelRes(mode: BlockMode): Int = when (mode) {
    BlockMode.NONE -> R.string.components_block_mode_none
    BlockMode.HARD_BLOCK -> R.string.components_block_mode_hard_block
    BlockMode.DELAY -> R.string.components_block_mode_delay
    BlockMode.HOLD -> R.string.components_block_mode_hold
    BlockMode.BREATHING -> R.string.components_block_mode_breathing
}

@Composable
internal fun blockModeLabel(mode: BlockMode): String = stringResource(blockModeLabelRes(mode))

/**
 * The label inside a four-segment picker row, where each segment is about a quarter of the screen.
 * One word each: "Hard Block" in a quarter-width segment with `maxLines = 1` was clipped to "Hard"
 * and, overflowing, drawn off-centre (device QA, groups PR 2).
 */
@StringRes
internal fun blockModeShortLabelRes(mode: BlockMode): Int = when (mode) {
    BlockMode.HARD_BLOCK -> R.string.components_block_mode_short_hard_block
    BlockMode.BREATHING -> R.string.components_block_mode_short_breathing
    BlockMode.NONE, BlockMode.DELAY, BlockMode.HOLD -> blockModeLabelRes(mode)
}

@Composable
internal fun blockModeShortLabel(mode: BlockMode): String = stringResource(blockModeShortLabelRes(mode))

/**
 * One line under the picker saying what the selected mode actually does. [manyApps] is true where
 * the rule covers several apps (a group, an All apps rule): Hard Block then reads "these apps", not
 * "the app". Required, so every picker states which it is (device QA screenshot review, PR 4).
 */
@StringRes
internal fun blockModeDescriptionRes(mode: BlockMode, manyApps: Boolean): Int = when (mode) {
    BlockMode.NONE -> R.string.components_block_mode_desc_none
    BlockMode.HARD_BLOCK ->
        if (manyApps) R.string.components_block_mode_desc_hard_block_many else R.string.components_block_mode_desc_hard_block_one
    BlockMode.DELAY -> R.string.components_block_mode_desc_delay
    BlockMode.HOLD -> R.string.components_block_mode_desc_hold
    BlockMode.BREATHING -> R.string.components_block_mode_desc_breathing
}

@Composable
internal fun blockModeDescription(mode: BlockMode, manyApps: Boolean): String =
    stringResource(blockModeDescriptionRes(mode, manyApps))
