package com.astraedus.nudge.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.runtime.Composable
import com.astraedus.nudge.R
import com.astraedus.nudge.domain.ContentFilterUserList
import com.astraedus.nudge.domain.text.UiText
import com.astraedus.nudge.domain.lock.ContentFilterBlocklistWeakening

internal val BLOCKLIST_TITLE: Int = R.string.settings_blocklist_title
internal val BLOCKLIST_SUBTITLE: Int = R.string.settings_blocklist_subtitle

/** The same sentence with "Removing one requires the unlock challenge." added (Strict Mode on). */
internal val BLOCKLIST_SUBTITLE_STRICT: Int = R.string.settings_blocklist_subtitle_strict
internal val BLOCKLIST_UNLOCK_PROMPT: Int = R.string.settings_blocklist_unlock_prompt
internal val BLOCKLIST_INVALID: UiText =
    UiText.of(R.string.settings_list_invalid, ContentFilterUserList.MAX_ENTRY_LENGTH)
internal val BLOCKLIST_DUPLICATE: UiText = UiText.of(R.string.settings_list_duplicate)
internal val BLOCKLIST_FULL: UiText = UiText.of(R.string.settings_list_full, ContentFilterUserList.MAX_ENTRIES)
internal val BLOCKLIST_EMPTY: Int = R.string.settings_blocklist_empty

private val BLOCKLIST_COPY = UserListCopy(
    title = BLOCKLIST_TITLE,
    icon = Icons.Outlined.Block,
    subtitle = BLOCKLIST_SUBTITLE,
    subtitleStrict = BLOCKLIST_SUBTITLE_STRICT,
    empty = BLOCKLIST_EMPTY
)

/**
 * The block list editor's one decision, the mirror of [allowlistSubmit]: adding a block is
 * strengthening, so it never needs the challenge. The gated direction is removal, which the host
 * screen handles before it calls the preference writer
 * ([com.astraedus.nudge.domain.lock.ContentFilterBlocklistWeakening]).
 */
internal fun blocklistSubmit(input: String, current: Set<String>): UserListSubmit {
    val entry = ContentFilterUserList.normalize(input)
        ?: return UserListSubmit.Rejected(BLOCKLIST_INVALID)
    if (entry in current) return UserListSubmit.Rejected(BLOCKLIST_DUPLICATE)
    if (current.size >= ContentFilterUserList.MAX_ENTRIES) {
        return UserListSubmit.Rejected(BLOCKLIST_FULL)
    }
    return UserListSubmit.Add(entry = entry, needsUnlock = false)
}

/**
 * Removing one entry from the block list: the host screen's whole decision, pure so it is testable
 * without Compose. A removal that loses a block weakens the filter, so Strict Mode gates it; one
 * that changes nothing (entry absent, or already canonicalised away) does not.
 */
internal fun blocklistRemoveNeedsUnlock(
    entry: String,
    current: Set<String>,
    strictModeEnabled: Boolean
): Boolean = ContentFilterBlocklistWeakening.requiresUnlock(
    current = current,
    next = current - entry,
    strictModeEnabled = strictModeEnabled
)

/**
 * "Blocked words & sites": the user's own additions to the content filter. Shown whenever the
 * filter is on, deliberately independent of "Strict keyword matching", which ships off.
 */
@Composable
internal fun ContentFilterBlocklistEditor(
    entries: Set<String>,
    strictModeEnabled: Boolean,
    onAdd: (entry: String, needsUnlock: Boolean) -> Unit,
    onRemove: (String) -> Unit
) {
    UserListEditor(
        copy = BLOCKLIST_COPY,
        strictModeEnabled = strictModeEnabled,
        entries = entries,
        submit = { blocklistSubmit(it, entries) },
        onAdd = onAdd,
        onRemove = onRemove
    )
}
