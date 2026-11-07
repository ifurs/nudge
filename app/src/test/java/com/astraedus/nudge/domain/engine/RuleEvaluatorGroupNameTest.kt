package com.astraedus.nudge.domain.engine

import com.astraedus.nudge.testing.enText
import com.astraedus.nudge.testing.en
import com.astraedus.nudge.data.repository.RuleSnapshot
import com.astraedus.nudge.domain.model.BlockMode
import com.astraedus.nudge.testing.aBlockRule
import com.astraedus.nudge.testing.aBlockRuleData
import com.astraedus.nudge.testing.aGroupMember
import com.astraedus.nudge.testing.anAppGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * L1: a group rule is named after its group (spec PR 2). The name is what the block overlay shows
 * as "Rule: <name>", so a Delay on Instagram that comes from "Social" says "Social", not "Delay".
 */
class RuleEvaluatorGroupNameTest {

    private val evaluator = RuleEvaluator()

    /** The label the engine builds, as the block screen reads it in English. */
    private fun name(rule: com.astraedus.nudge.domain.model.BlockRuleData): String =
        evaluator.buildRuleName(rule).enText()

    /** The subject: a group rule (group 7, "Social"), everything else as the app ships it. */
    private val socialRule = aBlockRuleData(id = 1, groupId = 7L, groupName = "Social")

    /** The same rule aimed straight at Instagram instead of a group. */
    private val directRule = aBlockRuleData(id = 1, packageName = "com.instagram.android")

    @Test
    fun `a group rule is its group's name`() {
        assertEquals("Social", name(socialRule))
    }

    @Test
    fun `the label is data, not words`() {
        assertEquals(
            com.astraedus.nudge.domain.model.RuleLabel(
                subject = com.astraedus.nudge.domain.model.RuleLabel.Subject.Group("Social"),
                scheduled = true,
                dailyLimitMinutes = 30
            ),
            evaluator.buildRuleName(socialRule.copy(scheduleDays = listOf(1), dailyLimitMinutes = 30))
        )
    }

    @Test
    fun `whatever the mode`() {
        for (mode in BlockMode.values()) {
            assertEquals("mode $mode", "Social", name(socialRule.copy(mode = mode)))
        }
    }

    @Test
    fun `the qualifiers stay`() {
        assertEquals("Social (scheduled)", name(socialRule.copy(scheduleDays = listOf(1, 2))))
        assertEquals("Social (30 min/day)", name(socialRule.copy(dailyLimitMinutes = 30)))
        assertEquals(
            "Social (scheduled, 30 min/day)",
            name(socialRule.copy(scheduleDays = listOf(1), dailyLimitMinutes = 30))
        )
    }

    @Test
    fun `a feature group rule keeps its feature prefix`() {
        assertEquals("Reels - Social", name(socialRule.copy(inAppFeatures = listOf("reels"))))
    }

    @Test
    fun `a group rule with no name is Group, never a mode name`() {
        assertEquals("Group", name(socialRule.copy(groupName = null)))
        assertEquals("Group", name(socialRule.copy(groupName = "  ")))
        assertEquals(
            "Group (30 min/day)",
            name(socialRule.copy(groupName = null, mode = BlockMode.HARD_BLOCK, dailyLimitMinutes = 30))
        )
    }

    @Test
    fun `a direct rule is unchanged`() {
        assertEquals("Delay", name(directRule))
        assertEquals(
            "Hard Block (30 min/day)",
            name(directRule.copy(mode = BlockMode.HARD_BLOCK, dailyLimitMinutes = 30))
        )
    }

    @Test
    fun `resolution carries the group name to the active rule`() {
        val resolved = evaluator.resolveRulesForPackage(
            "com.instagram.android",
            listOf(socialRule),
            membership(7L to "com.instagram.android"),
            CoverageFacts(essential = false, launchable = true, category = null, browser = false)
        )
        assertEquals("Social", resolved.single().ruleName.en())
    }

    /** The snapshot is where the name comes from: the groups table, by the rule's group id. */
    @Test
    fun `RuleSnapshot fills the group name from the groups table`() {
        val snapshot = RuleSnapshot.of(
            enabledRules = listOf(
                aBlockRule(id = 1, groupId = 7L),
                aBlockRule(id = 2, groupId = 8L),
                aBlockRule(id = 3, packageName = "com.example.app", mode = BlockMode.HARD_BLOCK.name)
            ),
            groups = listOf(anAppGroup("Social", id = 7L)),
            members = listOf(aGroupMember(7L, "com.instagram.android"))
        )
        val byId = snapshot.ruleData.associateBy { it.id }
        assertEquals("Social", byId.getValue(1).groupName)
        assertNull("a missing group row leaves the name null", byId.getValue(2).groupName)
        assertNull("a direct rule has no group name", byId.getValue(3).groupName)
        assertEquals("Group", name(byId.getValue(2)))
    }
}
