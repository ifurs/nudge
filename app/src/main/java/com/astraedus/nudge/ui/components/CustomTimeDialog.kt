package com.astraedus.nudge.ui.components

import com.astraedus.nudge.ui.text.asString
import com.astraedus.nudge.domain.text.UiText
import androidx.compose.ui.res.stringResource
import com.astraedus.nudge.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Dialog for entering a custom time value (seconds or minutes).
 * Validates input against min/max bounds and shows an error if out of range.
 */
@Composable
fun CustomTimeDialog(
    title: String,
    unit: String,
    currentValue: Int,
    min: Int,
    max: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentValue.toString()) }
    val parsed = text.toIntOrNull()
    val isValid = parsed != null && parsed in min..max
    val showError = text.isNotEmpty() && !isValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { newValue ->
                        // Allow only digits
                        if (newValue.all { it.isDigit() }) {
                            text = newValue
                        }
                    },
                    label = { Text(stringResource(R.string.components_custom_time_value_label, unit)) },
                    placeholder = { Text("$min-$max") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = showError,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showError) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.components_custom_time_range_error, min, max),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    parsed?.let { value ->
                        onConfirm(value.coerceIn(min, max))
                    }
                },
                enabled = isValid
            ) {
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

/**
 * Formats minutes into a human-readable time string.
 * Under 60 min: "Xm" (e.g. "45m")
 * Exactly 60 min multiples: "Xh" (e.g. "2h")
 * Mixed: "Xh Ym" (e.g. "1h 30m")
 */
fun minutesDisplayText(minutes: Int): UiText {
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return when {
        hours == 0 -> UiText.of(R.string.components_duration_short_minutes, remainingMinutes)
        remainingMinutes == 0 -> UiText.of(R.string.components_duration_short_hours, hours)
        else -> UiText.of(R.string.components_duration_short_hours_minutes, hours, remainingMinutes)
    }
}

@Composable
fun formatMinutesDisplay(minutes: Int): String = minutesDisplayText(minutes).asString()
