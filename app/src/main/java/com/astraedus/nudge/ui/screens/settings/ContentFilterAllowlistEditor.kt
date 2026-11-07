package com.astraedus.nudge.ui.screens.settings

import androidx.annotation.StringRes
import com.astraedus.nudge.domain.text.UiText
import com.astraedus.nudge.ui.text.asString
import androidx.compose.ui.res.stringResource
import com.astraedus.nudge.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.astraedus.nudge.domain.ContentFilterUserList
import com.astraedus.nudge.domain.lock.ContentFilterAllowlistWeakening

internal val ALLOWLIST_TITLE: Int = R.string.settings_allowlist_title
internal val ALLOWLIST_SUBTITLE: Int = R.string.settings_allowlist_subtitle

/** The same sentence with "Adding one requires the unlock challenge." added (Strict Mode on). */
internal val ALLOWLIST_SUBTITLE_STRICT: Int = R.string.settings_allowlist_subtitle_strict
internal val ALLOWLIST_UNLOCK_PROMPT: Int = R.string.settings_allowlist_unlock_prompt
internal val ALLOWLIST_INVALID: UiText =
    UiText.of(R.string.settings_list_invalid, ContentFilterUserList.MAX_ENTRY_LENGTH)
internal val ALLOWLIST_DUPLICATE: UiText = UiText.of(R.string.settings_list_duplicate)
internal val ALLOWLIST_FULL: UiText = UiText.of(R.string.settings_list_full, ContentFilterUserList.MAX_ENTRIES)
internal val ALLOWLIST_EMPTY: Int = R.string.settings_allowlist_empty

/** What submitting one of the user-owned list editors does. */
internal sealed interface UserListSubmit {
    /** Store [entry] (canonical form); first pass the Strict Mode challenge when [needsUnlock]. */
    data class Add(val entry: String, val needsUnlock: Boolean) : UserListSubmit

    /** Nothing is stored; show [message] under the field. */
    data class Rejected(val message: UiText) : UserListSubmit
}

/**
 * Copy for one user-list editor. The allow list and the block list are the same control; only the
 * words and the gated direction differ.
 */
internal data class UserListCopy(
    @StringRes val title: Int,
    val icon: ImageVector,
    @StringRes val subtitle: Int,
    /** [subtitle] plus the sentence that says the gated direction needs the challenge. */
    @StringRes val subtitleStrict: Int,
    @StringRes val empty: Int
)

private val ALLOWLIST_COPY = UserListCopy(
    title = ALLOWLIST_TITLE,
    icon = Icons.Outlined.TaskAlt,
    subtitle = ALLOWLIST_SUBTITLE,
    subtitleStrict = ALLOWLIST_SUBTITLE_STRICT,
    empty = ALLOWLIST_EMPTY
)

/**
 * The allow list editor's one decision, pure so it is testable without Compose: is [input]
 * storable, and does storing it need the unlock challenge. Adding an exception weakens the filter,
 * so under Strict Mode it does ([ContentFilterAllowlistWeakening]); removals never reach this, they
 * are always free.
 */
internal fun allowlistSubmit(
    input: String,
    current: Set<String>,
    strictModeEnabled: Boolean
): UserListSubmit {
    val entry = ContentFilterUserList.normalize(input)
        ?: return UserListSubmit.Rejected(ALLOWLIST_INVALID)
    if (entry in current) return UserListSubmit.Rejected(ALLOWLIST_DUPLICATE)
    if (current.size >= ContentFilterUserList.MAX_ENTRIES) {
        return UserListSubmit.Rejected(ALLOWLIST_FULL)
    }
    return UserListSubmit.Add(
        entry = entry,
        needsUnlock = ContentFilterAllowlistWeakening.requiresUnlock(
            current, current + entry, strictModeEnabled
        )
    )
}

/**
 * "Allowed words & sites" (issue #78): the user's exceptions to strict keyword matching. A text
 * field to add one, and a chip per entry that removes it on tap.
 *
 * [onAdd] receives a canonical entry and whether it must pass the challenge first; the host screen
 * owns the challenge dialog, exactly as it does for its gated toggles.
 */
@Composable
internal fun ContentFilterAllowlistEditor(
    entries: Set<String>,
    strictModeEnabled: Boolean,
    onAdd: (entry: String, needsUnlock: Boolean) -> Unit,
    onRemove: (String) -> Unit
) {
    UserListEditor(
        copy = ALLOWLIST_COPY,
        strictModeEnabled = strictModeEnabled,
        entries = entries,
        submit = { allowlistSubmit(it, entries, strictModeEnabled) },
        onAdd = onAdd,
        onRemove = onRemove
    )
}

/**
 * One user-owned word/site list: a text field to add an entry, and a chip per entry that removes it
 * on tap. The caller supplies the copy and the submit policy, so the allow list and the block list
 * cannot drift apart in behaviour.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun UserListEditor(
    copy: UserListCopy,
    strictModeEnabled: Boolean,
    entries: Set<String>,
    submit: (String) -> UserListSubmit,
    onAdd: (entry: String, needsUnlock: Boolean) -> Unit,
    onRemove: (String) -> Unit
) {
    var input by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<UiText?>(null) }
    val sorted = remember(entries) { entries.sorted() }

    // Clear the field once what it holds has actually been stored. Not on submit: under Strict Mode
    // an add waits on the challenge, and a cancelled challenge should not eat what the user typed.
    LaunchedEffect(entries) {
        if (ContentFilterUserList.normalize(input) in entries) input = ""
    }

    fun submitInput() {
        when (val result = submit(input)) {
            is UserListSubmit.Add -> onAdd(result.entry, result.needsUnlock)
            is UserListSubmit.Rejected -> error = result.message
        }
    }

    Column {
        ListItem(
            headlineContent = { Text(stringResource(copy.title)) },
            supportingContent = {
                Text(stringResource(if (strictModeEnabled) copy.subtitleStrict else copy.subtitle))
            },
            leadingContent = { Icon(copy.icon, contentDescription = null) }
        )
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = {
                    input = it
                    error = null
                },
                label = { Text(stringResource(R.string.settings_list_field_label)) },
                placeholder = { Text(stringResource(R.string.settings_list_field_placeholder)) },
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { message -> { Text(message.asString()) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submitInput() }),
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { submitInput() }, enabled = input.isNotBlank()) { Text(stringResource(R.string.components_action_add)) }
        }
        if (sorted.isEmpty()) {
            Text(
                stringResource(copy.empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        } else {
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sorted.forEach { entry ->
                    InputChip(
                        selected = false,
                        onClick = { onRemove(entry) },
                        label = { Text(entry) },
                        trailingIcon = {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.settings_list_remove_entry, entry),
                                modifier = Modifier.size(InputChipDefaults.IconSize)
                            )
                        }
                    )
                }
            }
        }
    }
}
