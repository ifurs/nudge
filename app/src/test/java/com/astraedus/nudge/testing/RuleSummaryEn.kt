package com.astraedus.nudge.testing

import com.astraedus.nudge.data.db.entity.BlockRule
import com.astraedus.nudge.domain.model.BlockRuleData
import com.astraedus.nudge.ui.components.rule.RuleSummary

/**
 * [RuleSummary] as the English reader sees it: the same calls, resolved against the shipped
 * `res/values/` strings. The summary tests import this `as RuleSummary`, so they keep asserting the
 * exact English line a list row shows (and so pin the copy in `strings_components.xml`) while the
 * production object returns resource ids.
 */
object RuleSummaryEn {
    fun buildSummaryText(rules: List<BlockRule>): String = RuleSummary.buildSummaryText(rules).en()

    fun coveredByText(groupNames: List<String>): String? = RuleSummary.coveredByText(groupNames)?.en()

    fun groupTitle(name: String, memberCount: Int): String = RuleSummary.groupTitle(name, memberCount).en()

    fun allAppsTitle(allowedNames: List<String>): String = RuleSummary.allAppsTitle(allowedNames).en()

    fun allAppsTitle(
        rule: BlockRule,
        groupNames: Map<Long, String>,
        appLabels: Map<String, String>
    ): String = RuleSummary.allAppsTitle(rule, groupNames, appLabels).en()

    fun allAppsTitle(rule: BlockRuleData, appLabels: Map<String, String>): String =
        RuleSummary.allAppsTitle(rule, appLabels).en()

    fun allAppsSummaryText(rule: BlockRule): String = RuleSummary.allAppsSummaryText(rule).en()

    fun untilText(clockText: String): String = RuleSummary.untilText(clockText).en()

    fun groupSummaryText(ownRule: BlockRule?): String = RuleSummary.groupSummaryText(ownRule).en()

    fun scheduleText(rule: BlockRule): String? = RuleSummary.scheduleText(rule)?.en()

    fun scheduleText(days: List<Int>?, startMinute: Int?, endMinute: Int?): String? =
        RuleSummary.scheduleText(days, startMinute, endMinute)?.en()

    fun firstUnlockText(days: List<Int>?, minutes: Int): String = RuleSummary.firstUnlockText(days, minutes).en()
}
