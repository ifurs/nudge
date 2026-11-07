package com.astraedus.nudge.ui.screens.settings

import androidx.compose.ui.res.stringResource
import com.astraedus.nudge.R
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.astraedus.nudge.BuildConfig
import com.astraedus.nudge.ui.hasGrayscalePermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * The `adb` command the guide tells the user to run. Takes the applicationId (`dev.astraedus.nudge`),
 * NOT the class namespace (`com.astraedus.nudge`): `pm grant` resolves a package, and the namespace
 * is not one (#114).
 */
internal fun grayscaleAdbCommand(applicationId: String): String =
    "adb shell pm grant $applicationId android.permission.WRITE_SECURE_SETTINGS"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrayscaleGuideScreen(
    onNavigateBack: () -> Unit
) {
    val grantCommand = grayscaleAdbCommand(BuildConfig.APPLICATION_ID)
    val context = LocalContext.current
    val permissionGranted by remember { mutableStateOf(hasGrayscalePermission(context)) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_gs_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.components_action_back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Status indicator
            PermissionStatusCard(granted = permissionGranted)

            if (permissionGranted) {
                // Permission already granted -- show success only
                Spacer(Modifier.height(16.dp))
            } else {
                // Explanation
                Text(
                    stringResource(R.string.settings_gs_intro_1),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    stringResource(R.string.settings_gs_intro_2),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Option 1: Wireless
                SectionHeader(stringResource(R.string.settings_gs_option_1))

                NumberedStep(1, stringResource(R.string.settings_gs_w_step_1))
                NumberedStep(2, stringResource(R.string.settings_gs_w_step_2))
                NumberedStep(3, stringResource(R.string.settings_gs_w_step_3))
                NumberedStep(4, stringResource(R.string.settings_gs_w_step_4))

                AdbCommandCard(
                    command = "adb pair <ip>:<port>",
                    label = stringResource(R.string.settings_gs_pair_label),
                    snackbarHostState = snackbarHostState,
                    scope = scope,
                    context = context
                )

                NumberedStep(5, stringResource(R.string.settings_gs_w_step_5))

                AdbCommandCard(
                    command = grantCommand,
                    snackbarHostState = snackbarHostState,
                    scope = scope,
                    context = context
                )

                Spacer(Modifier.height(8.dp))

                // Option 2: With a Computer
                SectionHeader(stringResource(R.string.settings_gs_option_2))

                NumberedStep(1, stringResource(R.string.settings_gs_c_step_1))
                NumberedStep(2, stringResource(R.string.settings_gs_c_step_2))
                NumberedStep(3, stringResource(R.string.settings_gs_c_step_3))
                NumberedStep(4, stringResource(R.string.settings_gs_c_step_4))

                AdbCommandCard(
                    command = grantCommand,
                    snackbarHostState = snackbarHostState,
                    scope = scope,
                    context = context
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    stringResource(R.string.settings_gs_done),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PermissionStatusCard(granted: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (granted) {
                Color(0xFF1B5E20).copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (granted) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50)
                )
                Column {
                    Text(
                        stringResource(R.string.settings_gs_granted_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        stringResource(R.string.settings_gs_granted_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    stringResource(R.string.settings_gs_not_granted_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    stringResource(R.string.settings_gs_not_granted_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun NumberedStep(number: Int, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "$number.",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AdbCommandCard(
    command: String,
    label: String? = null,
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope,
    context: Context
) {
    val clipboardLabel = stringResource(R.string.settings_gs_clipboard_label)
    val copiedText = stringResource(R.string.settings_gs_copied)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                command,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText(clipboardLabel, command))
                scope.launch {
                    snackbarHostState.showSnackbar(copiedText)
                }
            }) {
                Icon(
                    Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.settings_gs_copy_command),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (label != null) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
            )
        }
    }
}
