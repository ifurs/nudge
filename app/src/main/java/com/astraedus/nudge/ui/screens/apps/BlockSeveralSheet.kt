package com.astraedus.nudge.ui.screens.apps

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.astraedus.nudge.R
import com.astraedus.nudge.ui.components.TonalIcon
import com.astraedus.nudge.ui.components.rule.AllAppsIcon

/**
 * The two things "Block several apps" can make (spec 3.2): a group, or an All apps rule. The words
 * are literal: this is the one place a short description earns its place, because the choice is the
 * whole point of the sheet.
 */
enum class BlockSeveralOption(
    @StringRes val title: Int,
    @StringRes val subtitle: Int
) {
    GROUP(R.string.apps_block_several_group, R.string.apps_block_several_group_hint),
    ALL_APPS(R.string.apps_block_several_all_apps, R.string.apps_block_several_all_apps_hint);

    /** Where the row goes: the new-group flow (null id), or a new All apps rule (null id). */
    fun open(onOpenGroup: (Long?) -> Unit, onOpenAllAppsRule: (Long?) -> Unit) {
        when (this) {
            GROUP -> onOpenGroup(null)
            ALL_APPS -> onOpenAllAppsRule(null)
        }
    }

    val icon: ImageVector
        get() = when (this) {
            GROUP -> Icons.Default.Folder
            ALL_APPS -> AllAppsIcon
        }
}

/** The bottom sheet behind "Block several apps". Picking a row closes the sheet and opens it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockSeveralSheet(
    onPick: (BlockSeveralOption) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            Text(
                stringResource(R.string.apps_block_several),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
            BlockSeveralOption.entries.forEach { option ->
                ListItem(
                    headlineContent = { Text(stringResource(option.title)) },
                    supportingContent = { Text(stringResource(option.subtitle)) },
                    leadingContent = { TonalIcon(option.icon) },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.clickable { onPick(option) }
                )
            }
        }
    }
}
