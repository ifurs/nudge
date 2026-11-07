package com.astraedus.nudge.ui.screens.home

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import com.astraedus.nudge.data.db.entity.BlockRule
import com.astraedus.nudge.domain.lock.RuleWeakening
import com.astraedus.nudge.domain.model.BlockMode
import com.astraedus.nudge.ui.screens.config.UnifiedAppConfigState
import com.astraedus.nudge.ui.screens.config.isScheduledRule

/**
 * The short-video features the "Block short videos" card offers a switch for: which ones exist, what
 * state a switch is in, and the exact rule writes a switch makes. All pure Kotlin, so every decision
 * (including the Strict Mode one) is JVM-tested without a phone.
 *
 * ## One rule shape, shared with the app config screen
 *
 * A switch writes the very rows the app config screen's "Feature Rules" write
 * (`UnifiedAppConfigViewModel.performSave`): a direct, always-on rule with `inAppFeatures = <key>`
 * and mode Hard Block, sitting next to the app's own default rule. Anything that screen reads back
 * ("Feature Rules: Block") is what a switch here produced, and the other way round. The list of
 * features is `UnifiedAppConfigState.FEATURES_BY_PACKAGE` itself, so a new platform is one entry
 * there and a row here with no second edit.
 */

/** The feature a row blocks. [featureKey] is the value the rule's `inAppFeatures` column carries. */
enum class ShortVideoKind(val featureKey: String) {
    REELS("REELS"),
    SHORTS("SHORTS"),
    TIKTOK_FEED("TIKTOK_FEED"),

    /** Instagram Explore: a secondary row, listed after the three main ones. */
    EXPLORE("EXPLORE");

    companion object {
        fun ofKey(key: String): ShortVideoKind? = entries.firstOrNull { it.featureKey == key }
    }
}

/** One switch's subject: a feature of one app. */
@Immutable
data class ShortVideoTarget(val packageName: String, val kind: ShortVideoKind) {
    val featureKey: String get() = kind.featureKey

    /** Reels, Shorts and the TikTok feed are the card's reason to exist; Explore is a side option. */
    val primary: Boolean get() = kind != ShortVideoKind.EXPLORE
}

/** What a row's rule(s) amount to right now. */
enum class ShortVideoStatus {
    /** No rule that blocks this feature. */
    OFF,

    /** An enabled, always-on Hard Block for this feature: the switch is on. */
    BLOCKED,

    /**
     * An enabled always-on rule that slows the feature down (Delay, Hold, Breathing) instead of
     * blocking it. Set up in the app's own screen; the switch is off, and switching it on makes it a
     * full block.
     */
    SLOWED,

    /** Only a scheduled rule covers it: blocked at set times, free the rest of the day. */
    SCHEDULED
}

/** One row of the card, ready to draw. The icon is already rasterised (see `ShortVideoBlocksViewModel`). */
@Immutable
data class ShortVideoRow(
    val target: ShortVideoTarget,
    val appLabel: String,
    val icon: ImageBitmap?,
    val status: ShortVideoStatus
) {
    val blocked: Boolean get() = status == ShortVideoStatus.BLOCKED
    val key: String get() = "${target.packageName}/${target.featureKey}"
}

/** The card's whole state. */
@Immutable
data class ShortVideoState(
    val loaded: Boolean = false,
    val rows: List<ShortVideoRow> = emptyList(),
    /** Whether the accessibility service is bound; without it no switch has any effect. */
    val accessibilityWorking: Boolean = true
)

/** One change to the rules table. A plan is applied in order. */
sealed interface RuleWrite {
    data class Add(val rule: BlockRule) : RuleWrite
    data class Update(val rule: BlockRule) : RuleWrite
    data class Delete(val ruleId: Long) : RuleWrite
}

/**
 * What switching a row OFF does. [weakens] is the Strict Mode question (see [ShortVideoBlocks.planUnblock]).
 */
data class UnblockPlan(val writes: List<RuleWrite>, val weakens: Boolean)

object ShortVideoBlocks {

    /**
     * Every supported (app, feature) pair, main features first, in `FEATURES_BY_PACKAGE` order. A
     * feature the config screen offers but this card has no name for is left out rather than shown
     * with a blank title.
     */
    val TARGETS: List<ShortVideoTarget> = UnifiedAppConfigState.FEATURES_BY_PACKAGE
        .flatMap { (pkg, features) ->
            features.mapNotNull { feature ->
                ShortVideoKind.ofKey(feature.key)?.let { ShortVideoTarget(pkg, it) }
            }
        }
        .sortedBy { !it.primary }

    /** The features of [rule], split out of the comma-separated `inAppFeatures` column. */
    fun featuresOf(rule: BlockRule): List<String> =
        rule.inAppFeatures.orEmpty().split(",").map { it.trim() }.filter { it.isNotEmpty() }

    /** The app's own rules: aimed at the package itself, not at a group or at All apps. */
    private fun directRules(rules: List<BlockRule>, packageName: String): List<BlockRule> =
        rules.filter { it.packageName == packageName && it.groupId == null && !it.allApps }

    /** The always-on rules that name [target]'s feature: what the switch owns. */
    private fun alwaysOnRulesFor(rules: List<BlockRule>, target: ShortVideoTarget): List<BlockRule> =
        directRules(rules, target.packageName)
            .filter { !it.isScheduledRule() && target.featureKey in featuresOf(it) }

    private fun blocks(rule: BlockRule): Boolean = rule.enabled && rule.mode != BlockMode.NONE.name

    fun statusOf(rules: List<BlockRule>, target: ShortVideoTarget): ShortVideoStatus {
        val always = alwaysOnRulesFor(rules, target)
        return when {
            always.any { it.enabled && it.mode == BlockMode.HARD_BLOCK.name } -> ShortVideoStatus.BLOCKED
            always.any { blocks(it) } -> ShortVideoStatus.SLOWED
            directRules(rules, target.packageName).any {
                it.isScheduledRule() && target.featureKey in featuresOf(it) && blocks(it)
            } -> ShortVideoStatus.SCHEDULED
            else -> ShortVideoStatus.OFF
        }
    }

    /**
     * The writes for switching [target] ON: an always-on Hard Block for the feature.
     *
     * Leaves whole-app blocking exactly as it is. An app with no rule of its own gets one with mode
     * NONE, because that is what the config screen reads an app with only a feature rule as
     * ("Block the whole app" off); without it the config screen would show the Delay default and its
     * next Save would start blocking the whole app. Counters and overlays stay off on that row so
     * the switch changes nothing but the feature.
     *
     * Adding or upgrading a block is never a weakening, so this plan has no Strict Mode question.
     */
    fun planBlock(rules: List<BlockRule>, target: ShortVideoTarget): List<RuleWrite> {
        val direct = directRules(rules, target.packageName)
        val carrying = alwaysOnRulesFor(rules, target)
        if (carrying.any { it.enabled && it.mode == BlockMode.HARD_BLOCK.name }) return emptyList()

        val exact = carrying.firstOrNull { featuresOf(it) == listOf(target.featureKey) }
        if (exact != null) {
            return listOf(RuleWrite.Update(exact.copy(mode = BlockMode.HARD_BLOCK.name, enabled = true)))
        }

        val default = direct.firstOrNull { featuresOf(it).isEmpty() && !it.isScheduledRule() }
        val writes = mutableListOf<RuleWrite>()
        if (default == null) writes += RuleWrite.Add(newDefaultRule(target.packageName))
        // A rule that names this feature among others (a hand-edited import) keeps the others and
        // gives this one up, so the new Hard Block is the only rule deciding it.
        carrying.forEach { shared ->
            val rest = featuresOf(shared) - target.featureKey
            writes += RuleWrite.Update(shared.copy(inAppFeatures = rest.joinToString(",")))
        }
        writes += RuleWrite.Add(
            BlockRule(
                packageName = target.packageName,
                mode = BlockMode.HARD_BLOCK.name,
                enabled = true,
                inAppFeatures = target.featureKey,
                showCounter = default?.showCounter ?: false,
                tabVanish = default?.tabVanish ?: true,
                keepPassOnShortTrips = default?.keepPassOnShortTrips ?: false
            )
        )
        return writes
    }

    /**
     * The writes for switching [target] OFF, and whether they weaken protection.
     *
     * Removing an enabled blocking rule is weakening exactly as disabling it is: the answer comes
     * from [RuleWeakening.isWeakening] over the rule and its disabled copy, the same function the
     * app config screen asks, so Strict Mode cannot judge this removal differently from there. A rule
     * that was already off protected nothing, so removing it is free.
     *
     * When the removal leaves the app with nothing but a bare "do not block the app" row (the one
     * switching ON may have written), that row goes too, so the app is not left counted as
     * "blocked" with nothing blocked. A row carrying a limit, websites or any other option is the
     * user's own and stays.
     */
    fun planUnblock(rules: List<BlockRule>, target: ShortVideoTarget): UnblockPlan {
        val carrying = alwaysOnRulesFor(rules, target)
        if (carrying.isEmpty()) return UnblockPlan(emptyList(), weakens = false)

        val writes = mutableListOf<RuleWrite>()
        val removedIds = mutableSetOf<Long>()
        carrying.forEach { rule ->
            val rest = featuresOf(rule) - target.featureKey
            if (rest.isEmpty()) {
                writes += RuleWrite.Delete(rule.id)
                removedIds += rule.id
            } else {
                writes += RuleWrite.Update(rule.copy(inAppFeatures = rest.joinToString(",")))
            }
        }

        val direct = directRules(rules, target.packageName)
        val remaining = direct.filter { it.id !in removedIds }
        val default = remaining.singleOrNull()
        if (default != null && isBareNoBlockRule(default)) writes += RuleWrite.Delete(default.id)

        val weakens = carrying.any { RuleWeakening.isWeakening(it, it.copy(enabled = false)) }
        return UnblockPlan(writes, weakens)
    }

    /** An app-level rule that blocks nothing and carries no option: the row ON writes for a new app. */
    internal fun isBareNoBlockRule(rule: BlockRule): Boolean =
        rule.inAppFeatures.isNullOrEmpty() &&
            !rule.isScheduledRule() &&
            rule.mode == BlockMode.NONE.name &&
            rule.dailyLimitMinutes == null &&
            rule.webDomains == null &&
            rule.autoKickAfter == null &&
            rule.autoKickAfterMinutes == null &&
            rule.remindAfterMinutes == null &&
            !rule.grayscale &&
            !rule.showCounter &&
            !rule.showTimeRemaining &&
            !rule.followingSteer

    private fun newDefaultRule(packageName: String) = BlockRule(
        packageName = packageName,
        mode = BlockMode.NONE.name,
        enabled = true,
        showCounter = false
    )

    /**
     * The card's rows for the apps that are installed, from the stored [rules]. [labels] maps an
     * installed package to its launcher name; [icons] resolves its rasterised icon (null = none).
     */
    fun rowsFor(
        labels: Map<String, String>,
        rules: List<BlockRule>,
        icons: (String) -> ImageBitmap? = { null }
    ): List<ShortVideoRow> = TARGETS
        .filter { it.packageName in labels }
        .map { target ->
            ShortVideoRow(
                target = target,
                appLabel = labels.getValue(target.packageName),
                icon = icons(target.packageName),
                status = statusOf(rules, target)
            )
        }
}
