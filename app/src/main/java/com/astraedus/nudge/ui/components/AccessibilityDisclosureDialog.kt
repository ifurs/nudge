package com.astraedus.nudge.ui.components

import androidx.compose.ui.res.stringResource
import com.astraedus.nudge.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Prominent disclosure dialog shown BEFORE requesting the Accessibility Service permission.
 *
 * Required by Google Play policy. The dialog explains:
 * - WHY the service is needed (detect foreground apps)
 * - WHAT data is accessed (package names only)
 * - HOW data is used (locally, never sent anywhere)
 *
 * Back press and tapping outside dismiss the dialog WITHOUT granting consent.
 */
@Composable
fun AccessibilityDisclosureDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.components_disclosure_title))
        },
        text = {
            Column {
                Text(
                    stringResource(R.string.components_disclosure_what),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.components_disclosure_why),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.components_disclosure_reads),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.components_disclosure_local),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.components_disclosure_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.components_disclosure_dismiss))
            }
        }
    )
}
