package com.astraedus.nudge.ui.screens.allapps

import com.astraedus.nudge.domain.text.UiText
import com.astraedus.nudge.ui.text.asString
import androidx.compose.ui.res.stringResource
import com.astraedus.nudge.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.astraedus.nudge.data.repository.InstalledAppsRepository
import com.astraedus.nudge.ui.components.AppListItem
import com.astraedus.nudge.ui.components.TonalIcon

/*
 * The All apps editor's own rows. The headings, "+ Add" row and When options both editors share
 * live in ui/components/rule/EditorRows.kt.
 */

/** "Enabled" with its switch, and the one number that says how big the rule is under it. */
@Composable
internal fun EnabledRow(enabled: Boolean, blocksText: UiText, onEnabledChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.allapps_enabled), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text(
                blocksText.asString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    }
}

/** An allowed group: tapping it opens the group editor, ✕ takes it off this list only. */
@Composable
internal fun AllowedGroupItem(row: AllowedGroupRow, onOpen: () -> Unit, onRemove: () -> Unit) {
    ListItem(
        headlineContent = { Text(row.title.asString(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = row.stillBlocked?.let { text -> { Text(text.asString()) } },
        leadingContent = { TonalIcon(Icons.Default.Folder) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.allapps_remove_item, row.title.asString()))
                }
            }
        },
        modifier = Modifier.clickable(onClick = onOpen)
    )
}

@Composable
internal fun AllowedAppItem(row: AllowedAppRow, onRemove: () -> Unit) {
    AppListItem(
        appName = row.appName,
        icon = row.icon,
        supportingText = row.alsoBlocked?.asString(),
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.allapps_remove_item, row.appName))
            }
        }
    )
}

/** The locked essentials row: no ✕ and no checkbox, because these can never be blocked here. */
@Composable
internal fun AlwaysAllowedItem(text: UiText, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(text.asString(), maxLines = 2, overflow = TextOverflow.Ellipsis) },
        leadingContent = { TonalIcon(Icons.Default.Lock) },
        trailingContent = {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

/**
 * "Always allowed": the essentials this phone can open, read-only (spec 3.4). Resolved on the
 * device, so it names this phone's dialer, messages app and so on, not generic names.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlwaysAllowedSheet(
    apps: List<InstalledAppsRepository.AppInfo>,
    onClose: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                stringResource(R.string.allapps_always_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Text(
                stringResource(R.string.allapps_always_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
            )
            apps.forEach { app -> AppListItem(appName = app.appName, icon = app.icon) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onClose) { Text(stringResource(R.string.components_action_close)) }
            }
        }
    }
}

/**
 * "Block N apps all the time?" (spec 3.4): asked before an All apps rule goes on as Hard block, all
 * the time, from the editor's Save and from Manage Apps' switch alike ([confirmsSwitchOn]).
 */
@Composable
fun AllTheTimeConfirmDialog(blockedCount: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(confirmTitle(blockedCount).asString()) },
        text = { Text(stringResource(ALL_THE_TIME_CONFIRM_BODY)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.allapps_confirm_block)) } },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.components_action_cancel)) }
        }
    )
}

/** The confirm's body, word for word from the spec. */
val ALL_THE_TIME_CONFIRM_BODY: Int = R.string.allapps_confirm_body
