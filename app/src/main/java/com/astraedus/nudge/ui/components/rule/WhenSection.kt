package com.astraedus.nudge.ui.components.rule

import com.astraedus.nudge.ui.text.asString
import com.astraedus.nudge.domain.text.UiText
import androidx.compose.ui.res.stringResource
import com.astraedus.nudge.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.text.format.DateFormat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.astraedus.nudge.ui.components.CustomTimeDialog
import com.astraedus.nudge.ui.components.formatMinutesDisplay
import java.util.Calendar

/** The three ways a rule can decide when it is active (spec 3.5). */
enum class ScheduleKind {
    ALL_TIME,
    DAYS_TIMES,
    FIRST_UNLOCK
}

/** Preset durations for "after first unlock", in minutes. */
private val FIRST_UNLOCK_PRESETS: List<Int> = listOf(15, 30, 60)

/** "15 min", "30 min", "1 h": the chip label of a first-unlock preset. */
private fun presetLabel(minutes: Int): UiText =
    if (minutes % 60 == 0) {
        UiText.of(R.string.components_duration_spaced_hours, minutes / 60)
    } else {
        UiText.of(R.string.components_duration_spaced_minutes, minutes)
    }

/**
 * The shared "When" section for the group editor and the All apps editor (spec 3.5).
 *
 * The caller owns all values; this composable is stateless except for the custom-time dialog and
 * the day-start dialog, which are internal to keep the surface small.
 */
@Composable
fun WhenSection(
    kind: ScheduleKind,
    onKindChange: (ScheduleKind) -> Unit,
    days: Set<Int>,
    onDaysChange: (Set<Int>) -> Unit,
    startHour: Int,
    startMinute: Int,
    onStartTimeChange: (Int, Int) -> Unit,
    endHour: Int,
    endMinute: Int,
    onEndTimeChange: (Int, Int) -> Unit,
    firstUnlockMinutes: Int,
    onFirstUnlockMinutesChange: (Int) -> Unit,
    dayStartMinute: Int,
    onDayStartClick: () -> Unit,
    startsAtNextUnlock: Boolean,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {}
) {
    Column(
        modifier = modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        EditorWhenOption(
            label = stringResource(R.string.components_when_all_time),
            selected = kind == ScheduleKind.ALL_TIME,
            onSelect = { onKindChange(ScheduleKind.ALL_TIME) }
        )
        EditorWhenOption(
            label = stringResource(R.string.components_when_days_times),
            selected = kind == ScheduleKind.DAYS_TIMES,
            onSelect = { onKindChange(ScheduleKind.DAYS_TIMES) }
        )
        if (kind == ScheduleKind.DAYS_TIMES) {
            DaysAndTimesFields(
                days = days,
                onDaysChange = onDaysChange,
                startHour = startHour,
                startMinute = startMinute,
                onStartTimeChange = onStartTimeChange,
                endHour = endHour,
                endMinute = endMinute,
                onEndTimeChange = onEndTimeChange
            )
        }
        EditorWhenOption(
            label = stringResource(R.string.components_when_first_unlock),
            selected = kind == ScheduleKind.FIRST_UNLOCK,
            onSelect = { onKindChange(ScheduleKind.FIRST_UNLOCK) }
        )
        if (kind == ScheduleKind.FIRST_UNLOCK) {
            FirstUnlockFields(
                days = days,
                onDaysChange = onDaysChange,
                firstUnlockMinutes = firstUnlockMinutes,
                onFirstUnlockMinutesChange = onFirstUnlockMinutesChange,
                dayStartMinute = dayStartMinute,
                onDayStartClick = onDayStartClick,
                startsAtNextUnlock = startsAtNextUnlock
            )
        }

        trailing()
    }
}

@Composable
internal fun FirstUnlockFields(
    days: Set<Int>,
    onDaysChange: (Set<Int>) -> Unit,
    firstUnlockMinutes: Int,
    onFirstUnlockMinutesChange: (Int) -> Unit,
    dayStartMinute: Int,
    onDayStartClick: () -> Unit,
    startsAtNextUnlock: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LabeledChipsRow(label = stringResource(R.string.components_when_for)) {
            FirstUnlockMinuteChips(
                minutes = firstUnlockMinutes,
                onSelect = onFirstUnlockMinutesChange
            )
        }

        LabeledChipsRow(label = stringResource(R.string.components_when_days)) {
            DayOfWeekChips(days = days, onDaysChange = onDaysChange)
        }

        DayStartRow(minute = dayStartMinute, onClick = onDayStartClick)

        if (startsAtNextUnlock) {
            Text(
                stringResource(R.string.components_when_starts_next_unlock),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LabeledChipsRow(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Top)
        )
        Column(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

/**
 * Minute presets for first-unlock rules (15 min / 30 min / 1 h) plus Custom.
 * Custom uses the existing [CustomTimeDialog] with minutes from 5 to 240.
 *
 * The chips use a compact Material-style toggle (filled when selected, outlined when not) laid out
 * in a single FlowRow. That keeps "Custom" from being orphaned on its own line on a 360dp phone
 * while preserving a 48dp minimum touch target.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FirstUnlockMinuteChips(
    minutes: Int,
    onSelect: (Int) -> Unit,
    customDialogTitle: String = stringResource(R.string.components_when_custom_duration)
) {
    var showDialog by remember { mutableStateOf(false) }
    val isCustom = FIRST_UNLOCK_PRESETS.none { it == minutes }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FIRST_UNLOCK_PRESETS.forEach { preset ->
            CompactFilterChip(
                selected = minutes == preset,
                onClick = { onSelect(preset) },
                label = { Text(presetLabel(preset).asString()) }
            )
        }
        CompactFilterChip(
            selected = isCustom,
            onClick = { showDialog = true },
            label = {
                Text(if (isCustom) formatMinutesDisplay(minutes) else stringResource(R.string.components_chip_custom))
            }
        )
    }

    if (showDialog) {
        CustomTimeDialog(
            title = customDialogTitle,
            unit = stringResource(R.string.components_unit_minutes),
            currentValue = minutes,
            min = 5,
            max = 240,
            onConfirm = {
                onSelect(it)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

/**
 * A compact toggle chip that keeps the Material [FilterChip] selected/unselected affordance
 * (filled + on-container colour when selected, outlined when not) while using less horizontal
 * padding so the first-unlock duration row fits on one narrow-phone line.
 */
@Composable
private fun CompactFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = MaterialTheme.shapes.small
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 48.dp)
            .clip(shape)
            .border(1.dp, borderColor, shape)
            .background(containerColor, shape)
            .toggleable(
                value = selected,
                onValueChange = { onClick() },
                role = Role.Checkbox
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.padding(horizontal = 4.dp, vertical = 6.dp)) {
            // The label lambda draws its own Text, so colour it through the content colour rather
            // than dropping the computed value on the floor (review nit).
            CompositionLocalProvider(LocalContentColor provides contentColor) { label() }
        }
    }
}

/**
 * The "New day starts at X" row. Tapping it fires [onClick]; the caller shows the picker.
 * The time is formatted with the device's 12/24-hour preference.
 */
@Composable
fun DayStartRow(
    minute: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val timeText = remember(minute) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minute / 60)
            set(Calendar.MINUTE, minute % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        DateFormat.getTimeFormat(context).format(calendar.time)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(R.string.components_when_new_day_starts_at, timeText),
            style = MaterialTheme.typography.bodyLarge
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Dialog that wraps the existing [TimeSelector] for picking the logical-day start minute.
 */
@Composable
fun DayStartDialog(
    minute: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableIntStateOf(minute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.components_when_new_day_starts)) },
        text = {
            TimeSelector(
                hour = selected / 60,
                minute = selected % 60,
                onTimeSelected = { hour, min -> selected = hour * 60 + min }
            )
        },
        confirmButton = {
            TextButton(onClick = { onPick(selected); onDismiss() }) {
                Text(stringResource(R.string.components_action_set))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.components_action_cancel))
            }
        }
    )
}
