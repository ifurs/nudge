package com.astraedus.nudge.ui.screens.home

import com.astraedus.nudge.data.db.entity.BlockRule
import com.astraedus.nudge.domain.model.BlockMode
import com.astraedus.nudge.testing.aBlockRule
import com.astraedus.nudge.ui.screens.config.UnifiedAppConfigState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The decisions behind the Home "Block short videos" card, with no phone and no database: which
 * state a switch is in, which rules switching it ON writes, which switching it OFF removes, and
 * whether that removal is a weakening (the Strict Mode question).
 */
class ShortVideoBlocksTest {

    private val instagram = "com.instagram.android"
    private val reels = ShortVideoTarget(instagram, ShortVideoKind.REELS)
    private val explore = ShortVideoTarget(instagram, ShortVideoKind.EXPLORE)

    private fun reelsRule(
        id: Long = 10,
        mode: BlockMode = BlockMode.HARD_BLOCK,
        enabled: Boolean = true,
        features: String = "REELS",
        scheduled: Boolean = false
    ) = aBlockRule(
        id = id,
        packageName = instagram,
        mode = mode.name,
        enabled = enabled,
        inAppFeatures = features,
        scheduleDays = if (scheduled) "1,2,3,4,5" else null,
        scheduleStartMinute = if (scheduled) 540 else null,
        scheduleEndMinute = if (scheduled) 1020 else null
    )

    private fun defaultRule(
        id: Long = 1,
        mode: BlockMode = BlockMode.NONE,
        dailyLimit: Int? = null,
        showCounter: Boolean = false
    ) = aBlockRule(
        id = id,
        packageName = instagram,
        mode = mode.name,
        dailyLimitMinutes = dailyLimit,
        showCounter = showCounter
    )

    // ── the targets ───────────────────────────────────────────────────────

    @Test
    fun `the card offers every feature the config screen offers, main ones first`() {
        val offered = ShortVideoBlocks.TARGETS.map { it.packageName to it.featureKey }.toSet()
        val configOffers = UnifiedAppConfigState.FEATURES_BY_PACKAGE
            .flatMap { (pkg, features) -> features.map { pkg to it.key } }
            .toSet()
        assertEquals(configOffers, offered)

        val firstSecondary = ShortVideoBlocks.TARGETS.indexOfFirst { !it.primary }
        assertTrue(
            "Explore is a side row: every main row comes before it",
            ShortVideoBlocks.TARGETS.drop(firstSecondary).none { it.primary }
        )
        assertEquals(ShortVideoKind.EXPLORE, ShortVideoBlocks.TARGETS[firstSecondary].kind)
    }

    @Test
    fun `rows exist only for installed apps`() {
        val rows = ShortVideoBlocks.rowsFor(mapOf(instagram to "Instagram"), emptyList())
        assertEquals(listOf(ShortVideoKind.REELS, ShortVideoKind.EXPLORE), rows.map { it.target.kind })
        assertTrue(ShortVideoBlocks.rowsFor(emptyMap(), emptyList()).isEmpty())
    }

    // ── status ────────────────────────────────────────────────────────────

    @Test
    fun `status reads the always-on rule for exactly that feature`() {
        assertEquals(ShortVideoStatus.OFF, ShortVideoBlocks.statusOf(emptyList(), reels))
        assertEquals(ShortVideoStatus.BLOCKED, ShortVideoBlocks.statusOf(listOf(reelsRule()), reels))
        assertEquals(ShortVideoStatus.OFF, ShortVideoBlocks.statusOf(listOf(reelsRule()), explore))
        assertEquals(
            "a rule that is switched off blocks nothing",
            ShortVideoStatus.OFF,
            ShortVideoBlocks.statusOf(listOf(reelsRule(enabled = false)), reels)
        )
        assertEquals(
            ShortVideoStatus.BLOCKED,
            ShortVideoBlocks.statusOf(listOf(reelsRule(features = "REELS,EXPLORE")), explore)
        )
    }

    @Test
    fun `a softer or scheduled rule is not shown as a full block`() {
        assertEquals(
            ShortVideoStatus.SLOWED,
            ShortVideoBlocks.statusOf(listOf(reelsRule(mode = BlockMode.DELAY)), reels)
        )
        assertEquals(
            ShortVideoStatus.SCHEDULED,
            ShortVideoBlocks.statusOf(listOf(reelsRule(scheduled = true)), reels)
        )
        assertEquals(
            "a whole-app rule is not a feature rule",
            ShortVideoStatus.OFF,
            ShortVideoBlocks.statusOf(listOf(defaultRule(mode = BlockMode.HARD_BLOCK)), reels)
        )
    }

    @Test
    fun `a group rule or another app's rule does not count`() {
        val group = aBlockRule(packageName = null, groupId = 5, mode = "HARD_BLOCK", inAppFeatures = "REELS")
        val other = aBlockRule(packageName = "com.other", mode = "HARD_BLOCK", inAppFeatures = "REELS")
        assertEquals(ShortVideoStatus.OFF, ShortVideoBlocks.statusOf(listOf(group, other), reels))
    }

    // ── switching ON ──────────────────────────────────────────────────────

    @Test
    fun `ON for a new app writes a no-block app row and a Hard Block for the feature`() {
        val writes = ShortVideoBlocks.planBlock(emptyList(), reels)

        val added = writes.map { (it as RuleWrite.Add).rule }
        assertEquals(2, added.size)
        val app = added[0]
        assertEquals("the app itself stays open", BlockMode.NONE.name, app.mode)
        assertEquals(null, app.inAppFeatures)
        assertFalse("no floating counter appears from a block switch", app.showCounter)
        val feature = added[1]
        assertEquals(BlockMode.HARD_BLOCK.name, feature.mode)
        assertEquals("REELS", feature.inAppFeatures)
        assertEquals(instagram, feature.packageName)
        assertEquals(null, feature.groupId)
        assertEquals(null, feature.scheduleDays)
        assertTrue(feature.enabled)
    }

    @Test
    fun `ON leaves an existing whole-app rule exactly as it is`() {
        val app = defaultRule(mode = BlockMode.DELAY, dailyLimit = 30, showCounter = true)

        val writes = ShortVideoBlocks.planBlock(listOf(app), reels)

        assertEquals(1, writes.size)
        val feature = (writes.single() as RuleWrite.Add).rule
        assertEquals(BlockMode.HARD_BLOCK.name, feature.mode)
        assertEquals("the rows of one app agree on the counter, as the config screen writes them", true, feature.showCounter)
    }

    @Test
    fun `ON upgrades a softer feature rule in place instead of adding a second`() {
        val delay = reelsRule(mode = BlockMode.DELAY, enabled = false)

        val writes = ShortVideoBlocks.planBlock(listOf(defaultRule(), delay), reels)

        val update = writes.single() as RuleWrite.Update
        assertEquals(delay.id, update.rule.id)
        assertEquals(BlockMode.HARD_BLOCK.name, update.rule.mode)
        assertTrue(update.rule.enabled)
    }

    @Test
    fun `ON when already blocked writes nothing`() {
        assertTrue(ShortVideoBlocks.planBlock(listOf(defaultRule(), reelsRule()), reels).isEmpty())
    }

    @Test
    fun `ON adds an always-on rule next to a scheduled one and leaves the schedule alone`() {
        val scheduled = reelsRule(id = 20, scheduled = true)

        val writes = ShortVideoBlocks.planBlock(listOf(defaultRule(), scheduled), reels)

        val added = (writes.single() as RuleWrite.Add).rule
        assertEquals(null, added.scheduleDays)
        assertEquals(BlockMode.HARD_BLOCK.name, added.mode)
    }

    @Test
    fun `ON for one feature of a shared rule takes it out of that rule`() {
        val shared = reelsRule(mode = BlockMode.DELAY, features = "REELS,EXPLORE")

        val writes = ShortVideoBlocks.planBlock(listOf(defaultRule(), shared), reels)

        assertEquals("EXPLORE", (writes[0] as RuleWrite.Update).rule.inAppFeatures)
        assertEquals("REELS", (writes[1] as RuleWrite.Add).rule.inAppFeatures)
    }

    @Test
    fun `ON is never a weakening of anything it touches`() {
        val soft = reelsRule(mode = BlockMode.BREATHING)
        val writes = ShortVideoBlocks.planBlock(listOf(defaultRule(), soft), reels)
        val updated = (writes.single() as RuleWrite.Update).rule
        assertFalse(com.astraedus.nudge.domain.lock.RuleWeakening.isWeakening(soft, updated))
    }

    // ── switching OFF ─────────────────────────────────────────────────────

    @Test
    fun `OFF removes the feature rule and, with nothing else left, the bare app row`() {
        val plan = ShortVideoBlocks.planUnblock(listOf(defaultRule(id = 1), reelsRule(id = 10)), reels)

        assertEquals(listOf<RuleWrite>(RuleWrite.Delete(10), RuleWrite.Delete(1)), plan.writes)
    }

    @Test
    fun `OFF keeps an app row that carries a limit, the counter or another feature`() {
        val withLimit = defaultRule(dailyLimit = 30)
        assertEquals(
            listOf<RuleWrite>(RuleWrite.Delete(10)),
            ShortVideoBlocks.planUnblock(listOf(withLimit, reelsRule()), reels).writes
        )
        val withCounter = defaultRule(showCounter = true)
        assertEquals(
            listOf<RuleWrite>(RuleWrite.Delete(10)),
            ShortVideoBlocks.planUnblock(listOf(withCounter, reelsRule()), reels).writes
        )
        val exploreRule = reelsRule(id = 11, features = "EXPLORE")
        assertEquals(
            listOf<RuleWrite>(RuleWrite.Delete(10)),
            ShortVideoBlocks.planUnblock(listOf(defaultRule(), reelsRule(), exploreRule), reels).writes
        )
    }

    @Test
    fun `OFF for a whole-app Delay app leaves the app rule alone`() {
        val plan = ShortVideoBlocks.planUnblock(
            listOf(defaultRule(mode = BlockMode.DELAY), reelsRule()),
            reels
        )
        assertEquals(listOf<RuleWrite>(RuleWrite.Delete(10)), plan.writes)
    }

    @Test
    fun `OFF for one feature of a shared rule keeps the others`() {
        val shared = reelsRule(features = "REELS,EXPLORE")
        val plan = ShortVideoBlocks.planUnblock(listOf(defaultRule(dailyLimit = 5), shared), reels)
        assertEquals("EXPLORE", (plan.writes.single() as RuleWrite.Update).rule.inAppFeatures)
        assertTrue(plan.weakens)
    }

    @Test
    fun `OFF never touches a scheduled rule`() {
        val scheduled = reelsRule(id = 20, scheduled = true)
        val plan = ShortVideoBlocks.planUnblock(listOf(defaultRule(), scheduled), reels)
        assertTrue(plan.writes.isEmpty())
        assertFalse(plan.weakens)
    }

    // ── the Strict Mode question ──────────────────────────────────────────

    @Test
    fun `removing an enabled block is a weakening, whatever its mode`() {
        BlockMode.entries.filter { it != BlockMode.NONE }.forEach { mode ->
            assertTrue(
                "removing a $mode block must take the Strict Mode challenge",
                ShortVideoBlocks.planUnblock(listOf(defaultRule(), reelsRule(mode = mode)), reels).weakens
            )
        }
    }

    @Test
    fun `removing a rule that was already off is free`() {
        val plan = ShortVideoBlocks.planUnblock(listOf(defaultRule(), reelsRule(enabled = false)), reels)
        assertFalse(plan.weakens)
        assertEquals(2, plan.writes.size)
    }

    @Test
    fun `OFF with nothing to remove is not a weakening`() {
        val plan = ShortVideoBlocks.planUnblock(listOf(defaultRule()), reels)
        assertFalse(plan.weakens)
        assertTrue(plan.writes.isEmpty())
    }

    @Test
    fun `a switch's rule is exactly the row the config screen reads back as Block`() {
        val written = ShortVideoBlocks.planBlock(emptyList(), reels)
            .map { (it as RuleWrite.Add).rule }
        val feature: BlockRule = written.last()
        assertEquals(
            com.astraedus.nudge.domain.model.FeatureMode.BLOCK,
            com.astraedus.nudge.domain.model.FeatureMode.fromBlockMode(feature.mode)
        )
    }
}
