package com.astraedus.nudge.ui.screens.home

import com.astraedus.nudge.domain.text.UiText
import com.astraedus.nudge.ui.text.asString
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astraedus.nudge.R
import com.astraedus.nudge.ui.components.AccessibilityDisclosureDialog
import com.astraedus.nudge.ui.components.StrictModeChallengeHost
import com.astraedus.nudge.ui.components.qaTag
import com.astraedus.nudge.ui.nuke.NukeUnlockHost
import com.astraedus.nudge.ui.screens.stats.charts.BlockedTrendChart
import com.astraedus.nudge.ui.screens.stats.charts.WeeklyBarChart
import com.astraedus.nudge.ui.testtags.UiTags

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    shortVideoViewModel: ShortVideoBlocksViewModel,
    onNavigateToApps: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToWillpower: () -> Unit = {},
    onNavigateToInterventions: () -> Unit = {},
    onNavigateToAppDetail: (String) -> Unit = {},
    onNavigateToNuke: () -> Unit = {},
    onOpenAllAppsRule: (Long) -> Unit = {},
    /** Opens an app's config screen, from a short-video row. */
    onOpenAppConfig: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val shortVideos by shortVideoViewModel.uiState.collectAsStateWithLifecycle()
    val shortVideoChallenge by shortVideoViewModel.challenge.collectAsStateWithLifecycle()
    var showAccessibilityDisclosure by remember { mutableStateOf(false) }
    val challenge by viewModel.challenge.collectAsStateWithLifecycle()
    val nukeSummary by viewModel.nukeSummary.collectAsStateWithLifecycle()
    val lockCard by viewModel.lockCard.collectAsStateWithLifecycle()
    val nukeUnlock by viewModel.nukeUnlock.collectAsStateWithLifecycle()

    StrictModeChallengeHost(
        challenge = challenge,
        onVerify = viewModel::verifyChallenge,
        onCancel = viewModel::cancelChallenge
    )

    // Switching a short-video block off under Strict Mode: the same unlock challenge as everywhere.
    StrictModeChallengeHost(
        challenge = shortVideoChallenge,
        onVerify = shortVideoViewModel::verifyChallenge,
        onCancel = shortVideoViewModel::cancelChallenge
    )

    // Play policy: the prominent disclosure comes BEFORE the system accessibility screen, here as
    // on Settings and in onboarding.
    if (showAccessibilityDisclosure) {
        AccessibilityDisclosureDialog(
            onConfirm = {
                showAccessibilityDisclosure = false
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
            onDismiss = { showAccessibilityDisclosure = false }
        )
    }

    NukeUnlockHost(
        state = nukeUnlock,
        onScanned = viewModel::onNukeScanned,
        onUseEmergencyCode = viewModel::useNukeEmergencyCode,
        onVerifyEmergency = viewModel::verifyNukeEmergency,
        onBackToChoice = viewModel::backToNukeChoice,
        onCancel = viewModel::cancelNukeUnlock
    )

    HomeContent(
        state = state,
        nukeSummary = nukeSummary,
        lockCard = lockCard,
        shortVideos = shortVideos,
        onToggleGlobalEnabled = { viewModel.toggleGlobalEnabled() },
        onToggleShortVideo = shortVideoViewModel::setBlocked,
        onShortVideoOpenApp = onOpenAppConfig,
        onEnableAccessibility = { showAccessibilityDisclosure = true },
        onNavigateToApps = onNavigateToApps,
        onNavigateToStats = onNavigateToStats,
        onNavigateToWillpower = onNavigateToWillpower,
        onNavigateToInterventions = onNavigateToInterventions,
        onNavigateToAppDetail = onNavigateToAppDetail,
        onNavigateToNuke = onNavigateToNuke,
        onOpenAllAppsRule = onOpenAllAppsRule
    )
}

/**
 * The dashboard drawn from plain state: no ViewModel, no flows. [HomeScreen] collects and passes
 * in; the screenshot gallery (`ui/gallery/`) renders this directly from fixture state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    state: HomeUiState,
    nukeSummary: NukeSummary,
    lockCard: LockCard?,
    shortVideos: ShortVideoState,
    onToggleGlobalEnabled: () -> Unit,
    onToggleShortVideo: (ShortVideoRow, Boolean) -> Unit,
    onShortVideoOpenApp: (String) -> Unit,
    onEnableAccessibility: () -> Unit,
    onNavigateToApps: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToWillpower: () -> Unit,
    onNavigateToInterventions: () -> Unit,
    onNavigateToAppDetail: (String) -> Unit,
    onNavigateToNuke: () -> Unit,
    onOpenAllAppsRule: (Long) -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.app_name_short),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    // TopAppBar leaves only 4dp after its last action; the cards below sit in a
                    // 16dp gutter, so the extra 12dp lines the switch up with them.
                    Switch(
                        checked = state.isGlobalEnabled,
                        onCheckedChange = { onToggleGlobalEnabled() },
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // First, directly under the header: "keep Instagram, lose Reels" is what most people
            // install Nudge for, and it used to be four taps away inside an app's config screen.
            ShortVideoCard(
                state = shortVideos,
                onToggle = onToggleShortVideo,
                onOpenApp = onShortVideoOpenApp,
                onEnableAccessibility = onEnableAccessibility
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    icon = Icons.Outlined.Schedule,
                    label = stringResource(R.string.home_tile_screen_time),
                    value = if (state.hasUsagePermission) state.todayTotalUsageFormatted.asString() else "--",
                    modifier = Modifier.weight(1f),
                    // Granted, this card used to be inert - the one tile showing a number the
                    // stats screen exists to explain, and tapping it did nothing. Ungranted, the
                    // label IS the call to action, which is why it is not also a subtitle.
                    action = if (state.hasUsagePermission) {
                        TileAction(stringResource(R.string.home_action_see_breakdown), onNavigateToStats)
                    } else {
                        TileAction(stringResource(R.string.home_action_tap_to_enable)) {
                            context.startActivity(
                                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                            )
                        }
                    }
                )
                StatCard(
                    icon = Icons.Outlined.Shield,
                    label = stringResource(R.string.home_tile_blocked_apps),
                    value = state.activeAppCount.toString(),
                    modifier = Modifier.weight(1f),
                    action = TileAction(stringResource(R.string.home_action_see_apps), onNavigateToApps)
                )
            }

            NukeCard(
                summary = nukeSummary,
                onClick = onNavigateToNuke
            )

            lockCard?.let { card ->
                LockCardTile(card = card, onClick = { onOpenAllAppsRule(card.ruleId) })
            }

            WeekAtAGlanceCard(
                charts = state.charts,
                weekTotalFormatted = state.weekTotalFormatted,
                hasUsagePermission = state.hasUsagePermission,
                onClick = onNavigateToStats
            )

            TopBlockedCard(
                apps = state.topBlocked,
                onNavigateToInterventions = onNavigateToInterventions,
                onNavigateToAppDetail = onNavigateToAppDetail
            )

            SectionHeader(
                title = stringResource(R.string.home_section_today),
                // Said once, on the first section only. Repeating it over "All Time" reads as
                // noise, and by then the chevrons have already taught the pattern.
                hint = stringResource(R.string.home_section_hint)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    icon = Icons.Outlined.Block,
                    label = stringResource(R.string.home_tile_blocked),
                    value = state.blockedCountToday.toString(),
                    modifier = Modifier.weight(1f),
                    action = TileAction(stringResource(R.string.home_action_patterns), onNavigateToInterventions)
                )
                StatCard(
                    icon = Icons.Outlined.ThumbUp,
                    label = stringResource(R.string.home_tile_walked_away),
                    value = state.changedMindCountToday.toString(),
                    modifier = Modifier.weight(1f),
                    action = TileAction(stringResource(R.string.home_action_willpower), onNavigateToWillpower)
                )
            }

            SectionHeader(title = stringResource(R.string.home_section_all_time))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    icon = Icons.Outlined.Block,
                    label = stringResource(R.string.home_tile_blocked),
                    value = state.allTimeBlockedCount.toString(),
                    modifier = Modifier.weight(1f),
                    action = TileAction(stringResource(R.string.home_action_patterns), onNavigateToInterventions)
                )
                StatCard(
                    icon = Icons.Outlined.ThumbUp,
                    label = stringResource(R.string.home_tile_walked_away),
                    value = state.allTimeChangedMindCount.toString(),
                    modifier = Modifier.weight(1f),
                    action = TileAction(stringResource(R.string.home_action_willpower), onNavigateToWillpower)
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * The dashboard's two mini charts: screen time and nudges over the last 7 days.
 *
 * Read-only on purpose — the charts are given no select-day callback, so they install no tap
 * handling and taps fall through to the card, opening the full stats screen rather than
 * starting a day-selection interaction the home screen has nowhere to display.
 * `ChartSelectionContractTest` pins that.
 */
@Composable
private fun WeekAtAGlanceCard(
    charts: HomeCharts,
    weekTotalFormatted: UiText,
    hasUsagePermission: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            // The device-QA handle: the driver reads the week's counts from this card's text.
            .qaTag(UiTags.WEEK_CARD),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.widget_top_blocked_range),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (hasUsagePermission) weekTotalFormatted.asString() else "--",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.home_week_open_stats),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (charts.isEmpty) {
                Text(
                    stringResource(R.string.home_week_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                return@Column
            }

            Spacer(Modifier.height(12.dp))
            WeeklyBarChart(
                days = charts.weeklyScreenTime,
                chartHeight = 64.dp
            )

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.home_week_nudges),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // The counts take what the label leaves, right-aligned and free to wrap: a long
                // translation here once squeezed the label above it to one letter per line.
                Text(
                    stringResource(R.string.home_week_counts, charts.weekBlocked, charts.weekWalkedAway),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            BlockedTrendChart(
                days = charts.weeklyTrend,
                chartHeight = 56.dp,
                showLegend = false
            )
        }
    }
}

/**
 * Where a dashboard tile goes, and what to call it.
 *
 * One value rather than two independently-nullable parameters, so "navigates but says nothing" is
 * not a state anyone can write. That combination was the entire bug: two insight screens with one
 * silent entry point each, undiscovered for five versions. It used to be held together by a
 * source-scanning test asserting every call site passed both; the type does it now, at compile time
 * and for free.
 */
@Immutable
private data class TileAction(val label: String, val onClick: () -> Unit)

/**
 * A dashboard tile.
 *
 * With an [action] the tile carries a REAL affordance: a chevron, the action's label naming where
 * it goes, and - since the app-wide ripple override was removed in this same change - visible touch
 * feedback. This answers a usability report that cost two whole screens their audience: "I had no
 * idea I could click the Blocked and Walked Away tiles." The one card the owner DID discover was
 * the one card with a chevron, so a chevron is what every navigating tile now gets.
 *
 * Without an [action] the tile renders none of it, so a non-interactive tile stays honest. The
 * label doubles as the `onClickLabel`, which is what TalkBack announces.
 */
@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    action: TileAction? = null
) {
    Card(
        modifier = if (action != null) {
            modifier.clickable(onClick = action.onClick, onClickLabel = action.label)
        } else {
            modifier
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                if (action != null) {
                    Text(
                        action.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (action != null) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.55f)
                )
            }
        }
    }
}

/**
 * A section heading, optionally with a one-line hint about what the tiles under it do.
 *
 * The hint exists because a chevron says "this goes somewhere" but not "there are charts
 * behind it", and the report this change answers was about charts nobody knew existed.
 */
@Composable
private fun SectionHeader(title: String, hint: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            // Without a hint the title takes the row; with one, the hint takes what the title
            // leaves (a long translation once squeezed the title to a letter per line).
            modifier = if (hint == null) Modifier.weight(1f) else Modifier
        )
        if (hint != null) {
            Text(
                hint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )
        }
    }
}
