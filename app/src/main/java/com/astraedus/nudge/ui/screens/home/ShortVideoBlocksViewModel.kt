package com.astraedus.nudge.ui.screens.home

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astraedus.nudge.R
import com.astraedus.nudge.data.db.entity.BlockRule
import com.astraedus.nudge.data.preferences.NudgePreferences
import com.astraedus.nudge.data.repository.BlockRuleRepository
import com.astraedus.nudge.data.repository.InstalledAppsRepository
import com.astraedus.nudge.di.IoDispatcher
import com.astraedus.nudge.domain.lock.ChallengeState
import com.astraedus.nudge.ui.lock.StrictModeGate
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The "Block short videos" card (Home) and the onboarding step that reuses it: which short-video
 * features of the installed apps are blocked, and the switches that change that.
 *
 * ## One write path
 *
 * A switch writes through [BlockRuleRepository] (`addRule`, `updateRule`, `deleteRule`), the same
 * calls `UnifiedAppConfigViewModel` makes, with rows shaped by [ShortVideoBlocks] to match what that
 * screen writes. There is no second way to store a feature block.
 *
 * ## Self-control
 *
 * Switching ON is always instant (a stronger rule is never gated). Switching OFF removes an enabled
 * block, which is a weakening: it goes through [StrictModeGate.run], so with Strict Mode on it waits
 * for the typed unlock challenge exactly like the app config screen's Save and Manage Apps' switch
 * (`docs/architecture/strict-mode.md`). Whether a removal weakens is [UnblockPlan.weakens], decided
 * by `RuleWeakening`, so a rule that was already off is not challenged.
 *
 * Nuke Mode is not consulted here: Nuke gates the master switch and its own list, and says so in
 * `docs/architecture/nuke-mode.md` ("a nuked app's own rules are simply overridden"); no per-app
 * rule edit in the app (config screen, Manage Apps, group editor) asks Nuke, so this switch does not
 * either. Asking only here would add friction a user escapes by opening the app's config screen.
 */
@HiltViewModel
class ShortVideoBlocksViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val blockRuleRepository: BlockRuleRepository,
    private val installedAppsRepository: InstalledAppsRepository,
    nudgePreferences: NudgePreferences,
    accessibilityHealth: AccessibilityHealth,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val strictModeGate = StrictModeGate(nudgePreferences)

    /** Active Strict Mode unlock challenge, if a switch-off is waiting for it. */
    val challenge: StateFlow<ChallengeState?> = strictModeGate.challenge

    /** Serialises writes: two quick taps must not both plan against the same stale rule list. */
    private val writeLock = Mutex()

    private val installedLabels = installedAppsRepository.changes.map {
        installedAppsRepository.getInstalledApps().associate { it.packageName to it.appName }
    }

    val uiState: StateFlow<ShortVideoState> = combine(
        installedLabels,
        blockRuleRepository.getAllRules(),
        accessibilityHealth.working
    ) { labels, rules, accessibilityWorking ->
        val relevant = labels.filterKeys { pkg -> ShortVideoBlocks.TARGETS.any { it.packageName == pkg } }
        val icons = relevant.keys.associateWith { pkg ->
            installedAppsRepository.resolveIcon(pkg)?.rasterise(iconPx())
        }
        ShortVideoState(
            loaded = true,
            rows = ShortVideoBlocks.rowsFor(relevant, rules) { icons[it] },
            accessibilityWorking = accessibilityWorking
        )
    }
        .flowOn(ioDispatcher)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShortVideoState())

    /** A switch was flipped. ON is instant; OFF is gated like every other weakening. */
    fun setBlocked(row: ShortVideoRow, blocked: Boolean) {
        viewModelScope.launch {
            val target = row.target
            if (blocked) {
                applyPlan(target.packageName) { ShortVideoBlocks.planBlock(it, target) }
                return@launch
            }
            val plan = ShortVideoBlocks.planUnblock(currentRules(target.packageName), target)
            val remove: suspend () -> Unit = {
                // Planned again from the rules as they are when it runs (after a challenge: when
                // it passes), not as they were when the switch was tapped.
                applyPlan(target.packageName) { ShortVideoBlocks.planUnblock(it, target).writes }
            }
            if (plan.weakens) {
                strictModeGate.run(
                    prompt = context.getString(R.string.home_short_unlock_prompt, row.appLabel),
                    action = remove
                )
            } else {
                remove()
            }
        }
    }

    private suspend fun currentRules(packageName: String): List<BlockRule> =
        blockRuleRepository.getRulesForPackage(packageName).first()

    private suspend fun applyPlan(packageName: String, plan: (List<BlockRule>) -> List<RuleWrite>) {
        writeLock.withLock {
            plan(currentRules(packageName)).forEach { write ->
                when (write) {
                    is RuleWrite.Add -> blockRuleRepository.addRule(write.rule)
                    is RuleWrite.Update -> blockRuleRepository.updateRule(write.rule)
                    is RuleWrite.Delete -> blockRuleRepository.deleteRule(write.ruleId)
                }
            }
        }
    }

    /** Called from the challenge dialog; runs the pending switch-off on an exact match. */
    fun verifyChallenge(input: String) {
        viewModelScope.launch { strictModeGate.verifyAndRun(input) }
    }

    /** Called when the user backs out of the challenge dialog: the switch stays on. */
    fun cancelChallenge() {
        strictModeGate.cancel()
    }

    private fun iconPx(): Int =
        (ICON_SIZE_DP * context.resources.displayMetrics.density).roundToInt().coerceAtLeast(1)

    private fun Drawable.rasterise(sizePx: Int): ImageBitmap? = try {
        toBitmap(width = sizePx, height = sizePx).asImageBitmap()
    } catch (_: Exception) {
        null
    }

    companion object {
        /** The row's icon box; the composable draws at this size (`ShortVideoBlockRows`). */
        const val ICON_SIZE_DP = 40f
    }
}
