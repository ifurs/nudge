package com.astraedus.nudge.ui.screens.config

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astraedus.nudge.R
import com.astraedus.nudge.domain.model.BlockMode
import com.astraedus.nudge.ui.components.blockModeDescription
import com.astraedus.nudge.domain.model.FeatureMode
import com.astraedus.nudge.ui.components.DurationInput
import com.astraedus.nudge.ui.components.MinutesField
import com.astraedus.nudge.ui.components.StrictModeChallengeHost
import com.astraedus.nudge.ui.components.UsageReminderSection
import androidx.compose.ui.graphics.vector.ImageVector
import com.astraedus.nudge.ui.components.TonalIcon
import com.astraedus.nudge.ui.components.rule.AllAppsIcon
import com.astraedus.nudge.ui.components.rule.BlockModeSegmentedRow
import com.astraedus.nudge.ui.components.rule.FirstUnlockFields
import com.astraedus.nudge.ui.components.rule.DailyLimitChips
import com.astraedus.nudge.ui.components.rule.DayStartDialog
import com.astraedus.nudge.ui.components.rule.InfoButton
import com.astraedus.nudge.ui.components.rule.ScheduleSection
import com.astraedus.nudge.ui.components.rule.WaitTimeChips
import com.astraedus.nudge.ui.hasGrayscalePermission
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UnifiedAppConfigScreen(
    viewModel: UnifiedAppConfigViewModel,
    onNavigateBack: () -> Unit,
    /** Opens a group that covers this app, from "Also covered by". */
    onOpenGroup: (Long) -> Unit,
    /** Opens an All apps rule that covers this app, from "Also covered by". */
    onOpenAllAppsRule: (Long) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val coveredBy by viewModel.coveredBy.collectAsStateWithLifecycle()
    val coveredByAllApps by viewModel.coveredByAllApps.collectAsStateWithLifecycle()
    val challenge by viewModel.challenge.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onNavigateBack()
    }

    StrictModeChallengeHost(
        challenge = challenge,
        onVerify = viewModel::verifyChallenge,
        onCancel = viewModel::cancelChallenge
    )

    UnifiedAppConfigContent(
        state = state,
        coveredBy = coveredBy,
        coveredByAllApps = coveredByAllApps,
        actions = remember(viewModel) { viewModel.asActions() },
        onNavigateBack = onNavigateBack,
        onOpenGroup = onOpenGroup,
        onOpenAllAppsRule = onOpenAllAppsRule
    )
}

/**
 * The app config screen drawn from plain state: no ViewModel, no flows. [UnifiedAppConfigScreen]
 * collects and passes in; the screenshot gallery (`ui/gallery/`) renders this from fixture state.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UnifiedAppConfigContent(
    state: UnifiedAppConfigState,
    coveredBy: List<CoveringGroup>,
    coveredByAllApps: List<CoveringAllApps>,
    actions: UnifiedAppConfigActions,
    onNavigateBack: () -> Unit,
    onOpenGroup: (Long) -> Unit,
    onOpenAllAppsRule: (Long) -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDayStartDialog by remember { mutableStateOf(false) }
    val grayscaleSetupMessage = stringResource(R.string.config_grayscale_setup)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(state.appName.ifEmpty { state.packageName })
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.config_back))
                    }
                },
                actions = {
                    TextButton(onClick = actions::save) {
                        Text(stringResource(R.string.config_save))
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ═══ MASTER TOGGLE ═══
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.config_enabled),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = state.enabled,
                    onCheckedChange = actions::setEnabled
                )
            }

            if (coveredBy.isNotEmpty() || coveredByAllApps.isNotEmpty()) {
                AlsoCoveredBy(
                    groups = coveredBy,
                    allApps = coveredByAllApps,
                    onOpenGroup = onOpenGroup,
                    onOpenAllAppsRule = onOpenAllAppsRule
                )
            }

            // ═══ FEATURE BLOCKS ═══
            // First, above "Always Active" and "Default Behavior": for Instagram, YouTube and
            // TikTok this is the section most people came for ("keep the app, lose the Reels"),
            // and it used to sit at the bottom of a screen-long form.
            if (state.supportsFeatures) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SectionHeader(featureHeadline(state), modifier = Modifier.weight(1f, fill = false))
                        InfoButton(stringResource(R.string.config_feature_rules_info))
                    }

                    // The whole-app block is set further down and is on by default for a new app, so
                    // a user who only picks Block here would still gate the entire app. Say so.
                    if (state.blocksWholeApp) {
                        Text(
                            stringResource(R.string.config_feature_whole_app_hint, state.appName),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    state.availableFeatures.forEach { feature ->
                        FeatureOverrideCard(
                            featureName = featureTitle(feature),
                            override = state.featureOverrides[feature.key] ?: FeatureOverride(),
                            onUpdate = { actions.setFeatureOverride(feature.key, it) }
                        )
                    }

                    // Tab Vanish -- covers the in-app tab (e.g. Instagram's Reels tab) while a rule
                    // covering that feature resolves to a hard block, so the icon disappears rather
                    // than merely refusing to open. Visibility asks the registry (supportsTabVanish),
                    // never a hardcoded package check.
                    if (state.supportsTabVanish) {
                        val vanishLabel = state.vanishableFeatureLabel ?: REELS_LABEL
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.config_tab_vanish_title, vanishLabel),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    stringResource(
                                        R.string.config_tab_vanish_description,
                                        vanishLabel,
                                        state.appName
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.tabVanish,
                                onCheckedChange = actions::setTabVanish
                            )
                        }
                    }

                    // Following steer -- experimental, default off. Opens Instagram's Following feed
                    // instead of Home. Visibility asks the registry (supportsFollowingSteer).
                    if (state.supportsFollowingSteer) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.config_following_title),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    stringResource(R.string.config_following_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.followingSteer,
                                onCheckedChange = actions::setFollowingSteer
                            )
                        }
                    }
                }

                HorizontalDivider()
            }

            if (!state.supportsFeatures) HorizontalDivider()

            // ═══ ALWAYS ACTIVE ═══
            SectionHeader(stringResource(R.string.config_section_always_active))

            // Daily Time Limit
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            stringResource(R.string.config_daily_limit),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        InfoButton(
                            // Honest about the one direction that does not work rather than letting
                            // the budget silently never count web time. The reverse DOES work and is
                            // worth saying: spending the budget in the app closes the website too.
                            stringResource(
                                if (state.webDomainEnabled) {
                                    R.string.config_daily_limit_info_web
                                } else {
                                    R.string.config_daily_limit_info
                                }
                            )
                        )
                    }
                    Switch(
                        checked = state.dailyLimitEnabled,
                        onCheckedChange = actions::setDailyLimitEnabled
                    )
                }

                if (state.dailyLimitEnabled) {
                    DailyLimitChips(
                        minutes = state.dailyLimitMinutes,
                        onSelect = actions::setDailyLimitMinutes,
                        customDialogTitle = stringResource(R.string.config_daily_limit_custom)
                    )

                    // Show time remaining sub-toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                stringResource(R.string.config_show_time_remaining),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            InfoButton(stringResource(R.string.config_show_time_remaining_info))
                        }
                        Switch(
                            checked = state.showTimeRemaining,
                            onCheckedChange = actions::setShowTimeRemaining
                        )
                    }
                }
            }

            // Interaction counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        stringResource(R.string.config_counter),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    InfoButton(stringResource(R.string.config_counter_info))
                }
                Switch(
                    checked = state.showCounter,
                    onCheckedChange = actions::setShowCounter
                )
            }

            // Grayscale
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            stringResource(R.string.config_grayscale),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        InfoButton(stringResource(R.string.config_grayscale_info))
                    }
                    Text(
                        // Honest about the limitation rather than promising a setting that
                        // silently does nothing: grayscale rides inside a BlockDecision.Block, so
                        // with the app itself unblocked only a feature override can trigger it.
                        // See BlockMode.NONE.
                        stringResource(
                            if (state.blocksWholeApp) {
                                R.string.config_grayscale_hint
                            } else {
                                R.string.config_grayscale_hint_features
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.grayscale,
                    onCheckedChange = { enabled ->
                        if (enabled && !hasGrayscalePermission(context)) {
                            scope.launch {
                                snackbarHostState.showSnackbar(grayscaleSetupMessage)
                            }
                        } else {
                            actions.setGrayscale(enabled)
                        }
                    }
                )
            }

            // Web domain blocking
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            stringResource(R.string.config_web_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        InfoButton(stringResource(R.string.config_web_info))
                    }
                    Switch(
                        checked = state.webDomainEnabled,
                        onCheckedChange = actions::setWebDomainEnabled
                    )
                }

                if (state.webDomainEnabled) {
                    OutlinedTextField(
                        value = state.webDomains,
                        onValueChange = actions::setWebDomains,
                        label = { Text(stringResource(R.string.config_web_domains_label)) },
                        placeholder = { Text(stringResource(R.string.config_web_domains_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3,
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        stringResource(R.string.config_web_subdomains),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Websites enforce independently of the app-level mode (issue #21). While the
                    // app itself is blocked they simply follow its mode; when it is not, there is
                    // no app-level mode to follow, so the website mode is chosen here instead of
                    // silently enforcing nothing (which is what used to happen).
                    if (state.blocksWholeApp) {
                        Text(
                            stringResource(R.string.config_web_same_mode),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            stringResource(R.string.config_web_mode),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        BlockModeSegmentedRow(
                            selected = state.webBlockMode,
                            onSelect = actions::setWebBlockMode
                        )
                        Text(
                            stringResource(
                                R.string.config_web_opens_normally,
                                state.appName,
                                blockModeDescription(state.webBlockMode, manyApps = false)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider()

            // ═══ DEFAULT BEHAVIOR ═══
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SectionHeader(stringResource(R.string.config_section_default))
                InfoButton(stringResource(R.string.config_section_default_info))
            }

            // Whether the app itself is gated at all. Off => the app-level rule is BlockMode.NONE,
            // so the app opens freely and only the feature overrides below apply. This is the
            // switch that makes "block only Shorts, leave YouTube alone" expressible.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.config_block_whole_app), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (state.blocksWholeApp) {
                            stringResource(R.string.config_whole_app_on, state.appName)
                        } else if (state.supportsFeatures) {
                            stringResource(R.string.config_whole_app_off_features, state.appName)
                        } else {
                            stringResource(R.string.config_whole_app_off, state.appName)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.blocksWholeApp,
                    onCheckedChange = { actions.setBlocksWholeApp(it) }
                )
            }

            // Block mode segmented button — only meaningful when the app itself is blocked.
            if (state.blocksWholeApp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BlockModeSegmentedRow(
                        selected = state.defaultMode,
                        onSelect = actions::setDefaultMode,
                        compact = true
                    )

                    Text(
                        blockModeDescription(state.defaultMode, manyApps = false),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Delay duration (if applicable). One `delaySeconds` is stored per rule and it feeds
            // whichever delay is live — the app's, or (with whole-app blocking off) the website
            // one — so the control has to stay reachable in the web-only case too, else a web
            // DELAY would be permanently stuck at whatever was last saved.
            if (state.showDelayDuration) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(state.delayDurationLabelRes),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    WaitTimeChips(
                        seconds = state.defaultDelaySeconds,
                        onSelect = actions::setDefaultDelaySeconds,
                        customDialogTitle = stringResource(
                            R.string.config_custom_duration_title,
                            stringResource(state.delayDurationLabelRes)
                        )
                    )
                }
            }

            // Keep the pass on short trips (#110), per app, opt-in. Only where opening the app
            // earns a pass (Delay, Hold, Breathing): a hard block has nothing to keep. Label only,
            // the details live behind the info button.
            if (state.showKeepPassOnShortTrips) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            stringResource(R.string.config_keep_pass),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        InfoButton(stringResource(R.string.config_keep_pass_info))
                    }
                    Switch(
                        checked = state.keepPassOnShortTrips,
                        onCheckedChange = actions::setKeepPassOnShortTrips
                    )
                }
            }

            // Auto-kick
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            stringResource(R.string.config_auto_kick),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        // The two triggers differ on the web: the timer measures browser time on
                        // the blocked site, while scrolls/taps arrive carrying the browser's
                        // package, not the site's, so they cannot be attributed to it.
                        InfoButton(
                            stringResource(
                                if (state.webDomainEnabled) {
                                    R.string.config_auto_kick_info_web
                                } else {
                                    R.string.config_auto_kick_info
                                }
                            )
                        )
                    }
                    Switch(
                        checked = state.defaultAutoKickEnabled,
                        onCheckedChange = actions::setDefaultAutoKickEnabled
                    )
                }

                if (state.defaultAutoKickEnabled) {
                    Column(
                        modifier = Modifier.padding(start = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            stringResource(R.string.config_auto_kick_either),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.config_auto_kick_by_interactions),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Switch(
                                checked = state.defaultAutoKickByInteractions,
                                onCheckedChange = actions::setDefaultAutoKickByInteractions
                            )
                        }

                        if (state.defaultAutoKickByInteractions) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    pluralStringResource(
                                    R.plurals.config_auto_kick_after_interactions,
                                    state.defaultAutoKickAfter,
                                    state.defaultAutoKickAfter
                                ),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Slider(
                                    value = state.defaultAutoKickAfter.toFloat(),
                                    onValueChange = { actions.setDefaultAutoKickAfter(it.toInt()) },
                                    valueRange = AUTO_KICK_MIN.toFloat()..AUTO_KICK_MAX.toFloat(),
                                    steps = 18,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(AUTO_KICK_MIN.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(AUTO_KICK_MAX.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        MinutesField(
                            value = state.defaultAutoKickAfterMinutesText,
                            onValueChange = actions::setDefaultAutoKickAfterMinutesText,
                            labelText = stringResource(R.string.config_auto_kick_minutes_label),
                            supportingText = stringResource(R.string.config_auto_kick_minutes_hint)
                        )

                        MinutesField(
                            value = state.defaultAutoKickCooldownMinutesText,
                            onValueChange = actions::setDefaultAutoKickCooldownMinutesText,
                            labelText = stringResource(R.string.config_cooldown),
                            supportingText = stringResource(R.string.config_cooldown_hint)
                        )
                    }
                }
            }

            // Usage reminder (#77): next to auto-kick, on the same clock, and never instead of it.
            UsageReminderSection(
                enabled = state.reminderEnabled,
                minutesText = state.remindAfterMinutesText,
                // The kick this rule will SAVE with, so the hint matches what the engine will do.
                kickMinutes = if (state.defaultAutoKickEnabled) {
                    DurationInput.resolveMinutes(
                        state.defaultAutoKickAfterMinutesText,
                        state.originalAutoKickAfterMinutes
                    )
                } else null,
                websitesConfigured = state.webDomainEnabled,
                onEnabledChange = actions::setReminderEnabled,
                onMinutesTextChange = actions::setRemindAfterMinutesText,
                info = { InfoButton(it) }
            )

            HorizontalDivider()

            // ═══ SCHEDULED OVERRIDE ═══
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SectionHeader(stringResource(R.string.config_section_schedule))
                InfoButton(stringResource(R.string.config_section_schedule_info))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.config_schedule_enable),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = state.scheduledOverrideEnabled,
                    onCheckedChange = actions::setScheduledOverrideEnabled
                )
            }

            if (state.scheduledOverrideEnabled) {
                val scheduleKinds = listOf(
                    stringResource(R.string.config_schedule_days_times) to false,
                    stringResource(R.string.config_schedule_first_unlock) to true
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    scheduleKinds.forEachIndexed { index, (label, isFirstUnlock) ->
                        SegmentedButton(
                            selected = state.scheduledOverrideFirstUnlock == isFirstUnlock,
                            onClick = { actions.setScheduledOverrideFirstUnlock(isFirstUnlock) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = scheduleKinds.size
                            )
                        ) {
                            Text(label)
                        }
                    }
                }

                if (!state.scheduledOverrideFirstUnlock) {
                    ScheduleSection(
                        days = state.scheduleDays,
                        onDaysChange = actions::setScheduleDays,
                        startHour = state.scheduleStartHour,
                        startMinute = state.scheduleStartMinute,
                        onStartTimeChange = actions::setScheduleStartTime,
                        endHour = state.scheduleEndHour,
                        endMinute = state.scheduleEndMinute,
                        onEndTimeChange = actions::setScheduleEndTime,
                        mode = state.scheduledMode,
                        onModeChange = actions::setScheduledMode,
                        waitSeconds = state.scheduledDelaySeconds,
                        onWaitSecondsChange = actions::setScheduledDelaySeconds
                    ) {
                        ScheduledFeatureOverridesSection(
                            state = state,
                            onUpdate = actions::setScheduledFeatureOverride
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        BlockModeSegmentedRow(
                            selected = state.scheduledMode,
                            onSelect = actions::setScheduledMode
                        )
                        if (state.scheduledMode != BlockMode.HARD_BLOCK) {
                            WaitTimeChips(
                                seconds = state.scheduledDelaySeconds,
                                onSelect = actions::setScheduledDelaySeconds,
                                customDialogTitle = stringResource(
                            R.string.config_custom_duration_title,
                            stringResource(state.delayDurationLabelRes)
                        )
                            )
                        }
                        // The same layout the group and All apps editors use, labels to the
                        // left: one first-unlock row for all three editors (review should-fix 3).
                        FirstUnlockFields(
                            days = state.scheduleDays,
                            onDaysChange = actions::setScheduleDays,
                            firstUnlockMinutes = state.firstUnlockMinutes,
                            onFirstUnlockMinutesChange = actions::setFirstUnlockMinutes,
                            dayStartMinute = state.dayStartMinute,
                            onDayStartClick = { showDayStartDialog = true },
                            startsAtNextUnlock = state.startsAtNextUnlock
                        )
                        ScheduledFeatureOverridesSection(
                            state = state,
                            onUpdate = actions::setScheduledFeatureOverride
                        )
                    }
                }
            }

            if (showDayStartDialog) {
                DayStartDialog(
                    minute = state.dayStartMinute,
                    onPick = actions::setDayStartMinute,
                    onDismiss = { showDayStartDialog = false }
                )
            }

            // ═══ DANGER ZONE ═══
            if (state.hasExistingRules) {
                HorizontalDivider()
                OutlinedButton(
                    onClick = actions::showDeleteConfirmation,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.config_remove_all))
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // Delete confirmation dialog
    if (state.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = actions::dismissDeleteConfirmation,
            title = { Text(stringResource(R.string.config_remove_all_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.config_remove_all_body,
                        state.appName.ifEmpty { state.packageName }
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        actions.dismissDeleteConfirmation()
                        actions.deleteAllRules()
                    }
                ) {
                    Text(stringResource(R.string.config_remove), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = actions::dismissDeleteConfirmation) {
                    Text(stringResource(R.string.config_cancel))
                }
            }
        )
    }
}

/**
 * Feature overrides inside a scheduled override, shared by the days-and-times branch and the
 * first-unlock branch.
 */
@Composable
private fun ScheduledFeatureOverridesSection(
    state: UnifiedAppConfigState,
    onUpdate: (String, FeatureOverride) -> Unit
) {
    if (!state.supportsFeatures) return
    Spacer(Modifier.height(8.dp))
    Text(
        stringResource(R.string.config_schedule_feature_overrides),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Medium
    )
    state.availableFeatures.forEach { feature ->
        FeatureOverrideCard(
            featureName = featureTitle(feature),
            override = state.scheduledFeatureOverrides[feature.key] ?: FeatureOverride(),
            onUpdate = { onUpdate(feature.key, it) }
        )
    }
}

// ═══ Reusable composables ═══

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium,
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeatureOverrideCard(
    featureName: String,
    override: FeatureOverride,
    onUpdate: (FeatureOverride) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                featureName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            // Mode selector
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FeatureMode.entries.forEach { mode ->
                    FilterChip(
                        selected = override.mode == mode,
                        onClick = { onUpdate(override.copy(mode = mode)) },
                        label = {
                            Text(
                                stringResource(featureModeLabel(mode)),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )
                }
            }

            // Expanded settings for every mode that spends a duration.
            if (override.mode.usesDuration) {
                // Delay duration chips
                WaitTimeChips(
                    seconds = override.delaySeconds,
                    onSelect = { onUpdate(override.copy(delaySeconds = it)) }
                )

                // Auto-kick toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.config_auto_kick), style = MaterialTheme.typography.bodySmall)
                    Switch(
                        checked = override.autoKickEnabled,
                        onCheckedChange = { onUpdate(override.copy(autoKickEnabled = it)) }
                    )
                }

                if (override.autoKickEnabled) {
                    Text(
                        pluralStringResource(
                            R.plurals.config_auto_kick_after_scrolls,
                            override.autoKickAfter,
                            override.autoKickAfter
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = override.autoKickAfter.toFloat(),
                        onValueChange = { onUpdate(override.copy(autoKickAfter = it.toInt())) },
                        valueRange = AUTO_KICK_MIN.toFloat()..AUTO_KICK_MAX.toFloat(),
                        steps = 18,
                        modifier = Modifier.fillMaxWidth()
                    )
                    MinutesField(
                        value = override.autoKickCooldownMinutesText,
                        onValueChange = { onUpdate(override.copy(autoKickCooldownMinutesText = it)) },
                        labelText = stringResource(R.string.config_cooldown)
                    )
                }
            }
        }
    }
}

/**
 * The words on an in-app FEATURE override chip. Separate from [blockModeLabel] only because
 * [FeatureMode] carries [FeatureMode.INHERIT], which is not a block mode at all; the rest read the
 * same as the app-level picker on purpose.
 */
@StringRes
private fun featureModeLabel(mode: FeatureMode): Int = when (mode) {
    FeatureMode.INHERIT -> R.string.config_mode_inherit
    FeatureMode.BLOCK -> R.string.config_mode_block
    FeatureMode.DELAY -> R.string.config_mode_delay
    FeatureMode.HOLD -> R.string.config_mode_hold
    FeatureMode.BREATHING -> R.string.config_mode_breathing
}

/**
 * The name of an in-app feature for the user. Reels and Shorts are brand names and stay as they
 * are; Explore and the TikTok feed are ordinary words and come from resources.
 */
@Composable
private fun featureTitle(feature: FeatureInfo): String = when (feature.key) {
    "EXPLORE" -> stringResource(R.string.config_feature_explore)
    "TIKTOK_FEED" -> stringResource(R.string.config_feature_tiktok_feed)
    else -> feature.displayName
}

/**
 * The headline of the feature section: "Block only Reels, keep the rest of Instagram". It names the
 * app's first feature (Reels for Instagram, Shorts for YouTube); TikTok's only one is its video feed.
 */
@Composable
private fun featureHeadline(state: UnifiedAppConfigState): String {
    val first = state.availableFeatures.first()
    val app = state.appName.ifEmpty { state.packageName }
    return if (first.key == "TIKTOK_FEED") {
        stringResource(R.string.config_features_headline_feed, app)
    } else {
        stringResource(R.string.config_features_headline, featureTitle(first), app)
    }
}

/** The label Tab Vanish falls back to when the registry names no feature; a brand name. */
private const val REELS_LABEL = "Reels"

/** The auto-kick interaction slider's range. */
private const val AUTO_KICK_MIN = 5
private const val AUTO_KICK_MAX = 100

/**
 * "Also covered by" (spec 3.7): the groups that block this app as well, so "why is it blocked when
 * its own switch is off?" answers itself. Read-only; each row opens that group.
 */
@Composable
private fun AlsoCoveredBy(
    groups: List<CoveringGroup>,
    allApps: List<CoveringAllApps>,
    onOpenGroup: (Long) -> Unit,
    onOpenAllAppsRule: (Long) -> Unit
) {
    Column {
        Text(
            stringResource(R.string.config_also_covered_by),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        groups.forEach { group ->
            // The same 40dp tonal folder a group row has on Manage Apps: one look per group.
            CoveringRow(Icons.Default.Folder, group.label) { onOpenGroup(group.groupId) }
        }
        allApps.forEach { rule ->
            CoveringRow(AllAppsIcon, rule.label) { onOpenAllAppsRule(rule.ruleId) }
        }
    }
}

@Composable
private fun CoveringRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TonalIcon(icon)
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

