package com.astraedus.nudge.ui.screens.home

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astraedus.nudge.R
import com.astraedus.nudge.ui.components.TonalIcon

/**
 * The switches of the "Block short videos" card, without the card around them: one row per
 * short-video feature of an installed app, a notice above them while the accessibility service is
 * off, and a one-line hint instead when none of the apps is installed.
 *
 * Stateless and previewable: the Home card and the onboarding step both draw it from a
 * [ShortVideoState] (`ShortVideoBlocksViewModel` produces it) and hand the taps back.
 *
 * @param onMoreOptions opens an app's own config screen; null hides the "More options" button (the
 *   onboarding step, where that screen is not reachable yet).
 */
@Composable
fun ShortVideoBlockRows(
    state: ShortVideoState,
    onToggle: (ShortVideoRow, Boolean) -> Unit,
    onMoreOptions: ((packageName: String) -> Unit)?,
    onEnableAccessibility: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (!state.accessibilityWorking) {
            AccessibilityNotice(onEnableAccessibility)
        }
        if (state.loaded && state.rows.isEmpty()) {
            Text(
                stringResource(R.string.home_short_none_installed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        state.rows.forEach { row ->
            ShortVideoRowItem(row, onToggle, onMoreOptions)
        }
    }
}

/** "Nudge can't see Reels yet" plus the button that starts the existing permission flow. */
@Composable
private fun AccessibilityNotice(onEnable: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Outlined.AccessibilityNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error
        )
        Text(
            stringResource(R.string.home_short_accessibility_off),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
    Button(
        onClick = onEnable,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Text(stringResource(R.string.home_short_accessibility_action))
    }
}

@Composable
private fun ShortVideoRowItem(
    row: ShortVideoRow,
    onToggle: (ShortVideoRow, Boolean) -> Unit,
    onMoreOptions: ((String) -> Unit)?
) {
    val title = stringResource(titleRes(row.target.kind), row.appLabel)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .toggleable(
                    value = row.blocked,
                    role = Role.Switch,
                    onValueChange = { onToggle(row, it) }
                )
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RowIcon(row)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    stringResource(subtitleRes(row.status), row.appLabel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            // The row is the switch (toggleable above), so this one draws state only and is not a
            // second focus target for TalkBack.
            Switch(checked = row.blocked, onCheckedChange = null)
        }
        if (onMoreOptions != null) {
            val moreLabel = stringResource(R.string.home_short_more_options_for, title)
            TextButton(
                onClick = { onMoreOptions(row.target.packageName) },
                modifier = Modifier
                    .padding(start = ICON_BOX_DP.dp + 16.dp - 12.dp)
                    .defaultMinSize(minHeight = 48.dp)
                    .semantics { contentDescription = moreLabel }
            ) {
                Text(stringResource(R.string.home_short_more_options))
            }
        }
    }
}

@Composable
private fun RowIcon(row: ShortVideoRow) {
    val icon = row.icon
    if (icon != null) {
        Image(
            bitmap = icon,
            contentDescription = null,
            modifier = Modifier.size(ICON_BOX_DP.dp)
        )
    } else {
        // No launcher icon (a fixture, or one PackageManager would not give us): the tonal glyph
        // other lists use for an app without one, so the title still lines up.
        TonalIcon(Icons.Outlined.SmartDisplay)
    }
}

@StringRes
private fun titleRes(kind: ShortVideoKind): Int = when (kind) {
    ShortVideoKind.REELS -> R.string.home_short_title_reels
    ShortVideoKind.SHORTS -> R.string.home_short_title_shorts
    ShortVideoKind.TIKTOK_FEED -> R.string.home_short_title_tiktok_feed
    ShortVideoKind.EXPLORE -> R.string.home_short_title_explore
}

@StringRes
private fun subtitleRes(status: ShortVideoStatus): Int = when (status) {
    ShortVideoStatus.BLOCKED -> R.string.home_short_status_blocked
    ShortVideoStatus.OFF -> R.string.home_short_status_off
    ShortVideoStatus.SLOWED -> R.string.home_short_status_slowed
    ShortVideoStatus.SCHEDULED -> R.string.home_short_status_scheduled
}

/** The icon box in dp; the ViewModel rasterises launcher icons at the same size. */
private const val ICON_BOX_DP = 40
