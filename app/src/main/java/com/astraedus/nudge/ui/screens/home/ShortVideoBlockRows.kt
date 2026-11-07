package com.astraedus.nudge.ui.screens.home

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
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
 * @param onOpenApp opens an app's own config screen from the row body; null makes the whole row the
 *   switch (the onboarding step, where that screen is not reachable yet).
 */
@Composable
fun ShortVideoBlockRows(
    state: ShortVideoState,
    onToggle: (ShortVideoRow, Boolean) -> Unit,
    onOpenApp: ((packageName: String) -> Unit)?,
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
            ShortVideoRowItem(row, onToggle, onOpenApp)
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

/**
 * One short-video switch. With [onOpenApp] it is Android Settings' split row: the body (icon,
 * title, subtitle, a chevron) opens that app's config screen, a thin divider, then the switch,
 * which only toggles. Without it (the onboarding step, where that screen is not reachable yet) the
 * whole row is the switch.
 */
@Composable
private fun ShortVideoRowItem(
    row: ShortVideoRow,
    onToggle: (ShortVideoRow, Boolean) -> Unit,
    onOpenApp: ((String) -> Unit)?
) {
    val title = stringResource(titleRes(row.target.kind), row.appLabel)
    if (onOpenApp == null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ROW_MIN_HEIGHT_DP.dp)
                .toggleable(
                    value = row.blocked,
                    role = Role.Switch,
                    onValueChange = { onToggle(row, it) }
                )
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RowBody(row, title, Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            // The row is the switch (toggleable above), so this one draws state only and is not a
            // second focus target for TalkBack.
            Switch(checked = row.blocked, onCheckedChange = null)
        }
        return
    }

    val openLabel = stringResource(R.string.home_short_open_settings, row.appLabel)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ROW_MIN_HEIGHT_DP.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The body: one target, named by its text ("Reels in Instagram. Blocked..."), and the
        // click label says where it goes. Its height follows the text, never under 64dp.
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = ROW_MIN_HEIGHT_DP.dp)
                .clickable(onClickLabel = openLabel, role = Role.Button) { onOpenApp(row.target.packageName) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RowBody(row, title, Modifier.weight(1f))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        VerticalDivider(
            modifier = Modifier
                .height(32.dp)
                .padding(horizontal = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        // The switch is its own target, named for the feature it controls, 56 x 64dp.
        Box(
            modifier = Modifier
                .size(width = SWITCH_TARGET_WIDTH_DP.dp, height = ROW_MIN_HEIGHT_DP.dp)
                .toggleable(
                    value = row.blocked,
                    role = Role.Switch,
                    onValueChange = { onToggle(row, it) }
                )
                .semantics { contentDescription = title },
            contentAlignment = Alignment.Center
        ) {
            Switch(checked = row.blocked, onCheckedChange = null)
        }
    }
}

/** Icon, title and status: the part of a row that says what it is. */
@Composable
private fun RowBody(row: ShortVideoRow, title: String, modifier: Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        RowIcon(row)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                stringResource(subtitleRes(row.status)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

/** A row's minimum height and the switch target's side: above the 48dp touch minimum. */
private const val ROW_MIN_HEIGHT_DP = 64

/** The switch target's width: the switch is 52dp, so the target is wider than 48dp and no wider. */
private const val SWITCH_TARGET_WIDTH_DP = 56
