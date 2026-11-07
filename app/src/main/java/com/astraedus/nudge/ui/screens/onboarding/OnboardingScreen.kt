package com.astraedus.nudge.ui.screens.onboarding

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PlayDisabled
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astraedus.nudge.R
import com.astraedus.nudge.ui.components.AccessibilityDisclosureDialog
import com.astraedus.nudge.ui.components.StrictModeChallengeHost
import com.astraedus.nudge.ui.screens.home.ShortVideoBlockRows
import com.astraedus.nudge.ui.screens.home.ShortVideoBlocksViewModel
import com.astraedus.nudge.ui.screens.home.ShortVideoRow
import com.astraedus.nudge.ui.screens.home.ShortVideoState
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    shortVideoViewModel: ShortVideoBlocksViewModel,
    onComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val shortVideos by shortVideoViewModel.uiState.collectAsStateWithLifecycle()
    val shortVideoChallenge by shortVideoViewModel.challenge.collectAsStateWithLifecycle()

    StrictModeChallengeHost(
        challenge = shortVideoChallenge,
        onVerify = shortVideoViewModel::verifyChallenge,
        onCancel = shortVideoViewModel::cancelChallenge
    )

    Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> PermissionsPage(
                    onGrantAccessibility = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onGrantOverlay = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    },
                    onGrantUsageStats = {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                )
                2 -> WhatToBlockPage(
                    state = shortVideos,
                    onToggle = shortVideoViewModel::setBlocked
                )
                3 -> ReadyPage()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (pagerState.currentPage < PAGE_COUNT - 1) {
                Button(
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.onboarding_next))
                }
            } else {
                Button(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.onboarding_get_started))
                }
            }
        }
    }
}

private const val PAGE_COUNT = 4

@Composable
private fun WelcomePage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.SelfImprovement,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(32.dp))
        Text(
            stringResource(R.string.app_name_short),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.onboarding_welcome_tagline),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.onboarding_welcome_body),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PermissionsPage(
    onGrantAccessibility: () -> Unit,
    onGrantOverlay: () -> Unit,
    onGrantUsageStats: () -> Unit
) {
    var showAccessibilityDisclosure by remember { mutableStateOf(false) }

    if (showAccessibilityDisclosure) {
        AccessibilityDisclosureDialog(
            onConfirm = {
                showAccessibilityDisclosure = false
                onGrantAccessibility()
            },
            onDismiss = {
                showAccessibilityDisclosure = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.onboarding_permissions_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.onboarding_permissions_intro),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        PermissionCard(
            icon = Icons.Outlined.Accessibility,
            title = stringResource(R.string.onboarding_accessibility_title),
            description = stringResource(R.string.onboarding_accessibility_description),
            onClick = { showAccessibilityDisclosure = true }
        )

        Spacer(Modifier.height(12.dp))

        PermissionCard(
            icon = Icons.Outlined.Layers,
            title = stringResource(R.string.onboarding_overlay_title),
            description = stringResource(R.string.onboarding_overlay_description),
            onClick = onGrantOverlay
        )

        Spacer(Modifier.height(12.dp))

        PermissionCard(
            icon = Icons.Outlined.QueryStats,
            title = stringResource(R.string.onboarding_usage_title),
            description = stringResource(R.string.onboarding_usage_description),
            onClick = onGrantUsageStats
        )
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * "What do you want to block?": the Home card's switches, offered once at the end of setup, so the
 * first thing a new user does is the thing they installed Nudge for.
 */
@Composable
private fun WhatToBlockPage(
    state: ShortVideoState,
    onToggle: (ShortVideoRow, Boolean) -> Unit
) {
    val context = LocalContext.current
    var showAccessibilityDisclosure by remember { mutableStateOf(false) }

    if (showAccessibilityDisclosure) {
        AccessibilityDisclosureDialog(
            onConfirm = {
                showAccessibilityDisclosure = false
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
            onDismiss = { showAccessibilityDisclosure = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Icon(
            Icons.Outlined.PlayDisabled,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.onboarding_block_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.onboarding_block_body),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        ShortVideoBlockRows(
            state = state,
            onToggle = onToggle,
            // The app's config screen is not reachable from setup; Home offers it later.
            onOpenApp = null,
            onEnableAccessibility = { showAccessibilityDisclosure = true }
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.onboarding_block_footer),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PermissionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onboarding_grant_permission))
            }
        }
    }
}

@Composable
private fun ReadyPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(32.dp))
        Text(
            stringResource(R.string.onboarding_ready_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.onboarding_ready_body),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
