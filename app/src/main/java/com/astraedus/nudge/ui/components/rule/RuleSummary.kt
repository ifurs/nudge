package com.astraedus.nudge.ui.components.rule

import com.astraedus.nudge.R
import com.astraedus.nudge.data.db.entity.BlockRule
import com.astraedus.nudge.domain.model.BlockRuleData
import com.astraedus.nudge.domain.model.ExceptCsv
import com.astraedus.nudge.domain.text.UiText

/**
 * The one way a rule is put into words on a list: "Delay 15s Mon–Fri 9:00–17:00 · 30min limit".
 *
 * Manage Apps (app rows and group rows) and App config's "Also covered by" all read their summary
 * lines from here, so one rule never reads two ways on two screens (spec 3.2, "one format
 * everywhere"). Pure: rule rows in, text out.
 */
object RuleSummary {

    private const val SEPARATOR = " · "

    /**
     * Every rule row of ONE target (an app's direct rules, or a group's own rule) as one line.
     *
     * Each rule reads as its mode, then its daily limit. A rule's schedule belongs to that rule, so
     * it follows the rule's mode with a plain space ("Hard Block Sat, Sun"); " · " only ever
     * separates one rule, or a limit, from the next. Joined by the separator instead, an app with
     * two rules read as two modes and a stray schedule ("Delay 30s · Hard Block · Sat, Sun",
     * orchestrator review of PR 3). An app's rules are ordered the way App config lays them out:
     * the always-on app rule, then scheduled app rules, then the per-feature rules
     * ("Reels: Hard Block").
     *
     * Examples:
     *   "Hard Block"
     *   "Delay 15s · 30min limit"
     *   "Delay 15s · Reels: Hard Block"
     *   "Hard Block Mon–Fri 9:00–17:00"
     *   "Delay 30s · Hard Block Sat, Sun" (an always-on rule and a weekend rule)
     *   "30min limit" (a NONE rule that only caps the day)
     *   "30min limit Sat, Sun" (the same, at weekends: the schedule follows the limit)
     */
    fun buildSummaryText(rules: List<BlockRule>): UiText {
        val (featureRules, appRules) = rules.partition { !it.inAppFeatures.isNullOrBlank() }
        val ordered = appRules.sortedBy { scheduleText(it) != null } + featureRules

        val parts = ordered.flatMap { rule ->
            val features = rule.inAppFeatures
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.joinToString("/") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }
            val mode = formatMode(rule.mode, rule.delaySeconds)
            val pieces = listOfNotNull(
                // A NONE rule with a limit is only a limit: "30min limit", not "Not blocked · 30min limit".
                when {
                    features != null -> UiText.of(R.string.components_summary_feature_mode, features, mode)
                    rule.mode == NONE && rule.dailyLimitMinutes != null -> null
                    else -> mode
                },
                rule.dailyLimitMinutes?.let { UiText.of(R.string.components_summary_limit_minutes, it) }
            )
            // The schedule joins the rule's first piece (its mode, or its limit when that is all it
            // is) with a space, so it can never read as a rule of its own.
            val schedule = scheduleText(rule) ?: return@flatMap pieces
            listOf(UiText.Joined(listOf(pieces.first(), schedule), " ")) + pieces.drop(1)
        }

        return if (parts.isEmpty()) UiText.of(R.string.components_summary_configured) else UiText.Joined(parts, SEPARATOR)
    }

    /**
     * The second line of an app that has no rule of its own but is in a group that is blocking:
     * "In Social", "In Social, Work", "In 3 groups". Null when no such group (the app is not
     * blocked, and the line must not suggest it is). [groupNames] are the covering groups in the
     * order they should read.
     */
    fun coveredByText(groupNames: List<String>): UiText? = when (groupNames.size) {
        0 -> null
        1, 2 -> UiText.plural(R.plurals.components_summary_in_groups_named, groupNames.size, groupNames.joinToString(", "))
        else -> UiText.plural(R.plurals.components_summary_in_groups_count, groupNames.size, groupNames.size)
    }

    /** A group row's title on Manage Apps: "Social · 9 apps", "Work · 1 app". */
    fun groupTitle(name: String, memberCount: Int): UiText =
        UiText.plural(R.plurals.components_summary_group_title, memberCount, name, memberCount)

    /**
     * An All apps rule's title, everywhere it is named (Manage Apps, the Home lock card, the block
     * screen's rule line, App config's "Also covered by"): "All apps", "All apps except Spotify",
     * "All apps except Work, Spotify", "All apps except Work, Spotify +3".
     *
     * [allowedNames] are the allowed groups' names, then the allowed apps' labels, in that order
     * (groups first, as the editor lists them). The caller passes only names it can resolve: an
     * allowed app that is not installed is left out, as the editor hides it.
     */
    fun allAppsTitle(allowedNames: List<String>): UiText = when (allowedNames.size) {
        0 -> UiText.of(R.string.components_summary_all_apps)
        1, 2 -> UiText.of(R.string.components_summary_all_apps_except, allowedNames.joinToString(", "))
        else -> UiText.of(
            R.string.components_summary_all_apps_except_more,
            allowedNames.take(2).joinToString(", "),
            allowedNames.size - 2
        )
    }

    /**
     * [allAppsTitle] for a rule row: its allowed groups that still exist ([groupNames], by id, in
     * the order the rule lists them), then its allowed apps that are installed ([appLabels], by
     * package). One place resolves the names, so Manage Apps, App config, the group editor and the
     * Home card cannot name one rule two ways.
     */
    fun allAppsTitle(
        rule: BlockRule,
        groupNames: Map<Long, String>,
        appLabels: Map<String, String>
    ): UiText = allAppsTitle(
        ExceptCsv.groupIds(rule.exceptGroupIds).mapNotNull { groupNames[it] } +
            ExceptCsv.packages(rule.exceptPackages).mapNotNull { appLabels[it] }
    )

    /** [allAppsTitle] for the domain form of a rule, whose allowed groups are already named. */
    fun allAppsTitle(rule: BlockRuleData, appLabels: Map<String, String>): UiText = allAppsTitle(
        rule.exceptGroupNames + rule.exceptPackages.mapNotNull { appLabels[it] }
    )

    /**
     * An All apps rule's one summary line: its mode and schedule in the words every rule uses
     * ("Hard Block Mon–Fri 9:00–17:00", "Delay 15s"; "All the time" is left out). An All apps
     * rule has no daily limit and no in-app features (spec 3.4), so nothing else can appear.
     */
    fun allAppsSummaryText(rule: BlockRule): UiText = buildSummaryText(listOf(rule))

    /**
     * "Until 8:42 AM" / "Until 17:00": the end of the window a scheduled block is in (spec 3.8, 3.9).
     * [clockText] is the time already formatted in the user's 12/24-hour setting (the caller has the
     * Android `DateFormat`; this stays pure).
     */
    fun untilText(clockText: String): UiText = UiText.of(R.string.components_summary_until, clockText)

    /**
     * A group row's one summary line: its own rule in the same words an app's rules use
     * ("Delay 15s · 30min limit"), or "Not blocked" for a group with no rule of its own (an
     * imported one, say). Whether the rule is on is the row's switch, not this text.
     */
    fun groupSummaryText(ownRule: BlockRule?): UiText =
        if (ownRule == null) UiText.of(R.string.components_summary_not_blocked) else buildSummaryText(listOf(ownRule))

    /**
     * When [rule] is active, in words: "Mon–Fri 9:00–17:00", "Sat, Sun", "Every day 22:00–6:00".
     * Null when it is active all the time, which a summary leaves out (spec 3.2).
     *
     * Follows [com.astraedus.nudge.domain.engine.ScheduleEvaluator] exactly, so the words never
     * claim a window the engine does not enforce: a time window needs BOTH ends, and no days (or
     * all seven) means every day.
     */
    fun scheduleText(rule: BlockRule): UiText? {
        val days = rule.scheduleDays?.split(",")?.mapNotNull { it.trim().toIntOrNull() }
        val firstUnlockMinutes = rule.firstUnlockMinutes
        if (firstUnlockMinutes != null) return firstUnlockText(days, firstUnlockMinutes)
        return scheduleText(
            days = days,
            startMinute = rule.scheduleStartMinute,
            endMinute = rule.scheduleEndMinute
        )
    }

    /**
     * A first-unlock rule's window in words: "30min after first unlock", "1 h after first unlock on
     * Mon–Fri". Minutes below an hour, whole hours above it, so a 240-minute window reads "4 h"
     * rather than "240min" in a list where every other number is small.
     *
     * The days are the logical day's (spec 3.5), which is what the engine checks; no time range is
     * shown because the window's start is the user's own unlock, not a clock time.
     */
    fun firstUnlockText(days: List<Int>?, minutes: Int): UiText {
        val dayText = dayText(days)
        val duration = durationText(minutes)
        return if (dayText == null) {
            UiText.of(R.string.components_summary_first_unlock, duration)
        } else {
            UiText.of(R.string.components_summary_first_unlock_on_days, duration, dayText)
        }
    }

    /** "30min", "1 h", "4 h": minutes under an hour, whole hours above it. */
    private fun durationText(minutes: Int): UiText =
        if (minutes >= 60 && minutes % 60 == 0) {
            UiText.of(R.string.components_summary_duration_hours, minutes / 60)
        } else {
            UiText.of(R.string.components_summary_duration_minutes, minutes)
        }

    /** [scheduleText] on raw values; ISO days (1 = Mon .. 7 = Sun), minutes from midnight. */
    fun scheduleText(days: List<Int>?, startMinute: Int?, endMinute: Int?): UiText? {
        val dayText = dayText(days)
        val timeText = if (startMinute != null && endMinute != null) {
            "${clock(startMinute)}–${clock(endMinute)}"
        } else {
            null
        }
        return when {
            dayText == null && timeText == null -> null
            dayText == null -> UiText.of(R.string.components_summary_every_day, timeText!!)
            timeText == null -> dayText
            else -> UiText.Joined(listOf(dayText, UiText.Raw(timeText)), " ")
        }
    }

    /** "Mon–Fri", "Sat, Sun", "Mon, Wed, Fri", "Mon–Wed, Sat"; null for every day. */
    private fun dayText(days: List<Int>?): UiText? {
        val picked = days.orEmpty().filter { it in 1..7 }.toSortedSet()
        if (picked.isEmpty() || picked.size == 7) return null

        val runs = mutableListOf<IntRange>()
        for (day in picked) {
            val last = runs.lastOrNull()
            if (last != null && last.last == day - 1) runs[runs.lastIndex] = last.first..day
            else runs += day..day
        }
        // A run of three or more reads as a range; two days read better as a pair ("Sat, Sun").
        return UiText.Joined(
            runs.map { run ->
                if (run.last - run.first >= 2) {
                    UiText.of(R.string.components_summary_day_range, DAY_NAMES.getValue(run.first), DAY_NAMES.getValue(run.last))
                } else {
                    UiText.Joined(run.map { DAY_NAMES.getValue(it) }, ", ")
                }
            },
            ", "
        )
    }

    /** 540 -> "9:00", 1020 -> "17:00", 0 -> "0:00". */
    private fun clock(minute: Int): String {
        val m = minute.mod(24 * 60)
        return "${m / 60}:${(m % 60).toString().padStart(2, '0')}"
    }

    /** The mode in words. Every [com.astraedus.nudge.domain.model.BlockMode] has a branch. */
    private fun formatMode(mode: String, delaySeconds: Int): UiText = when (mode) {
        // The app itself is not gated; only this rule's feature overrides and daily limit are.
        NONE -> UiText.of(R.string.components_summary_not_blocked)
        "HARD_BLOCK" -> UiText.of(R.string.components_summary_mode_hard_block)
        "DELAY" -> UiText.of(R.string.components_summary_mode_delay, delaySeconds)
        "HOLD" -> UiText.of(R.string.components_summary_mode_hold, delaySeconds)
        "BREATHING" -> UiText.of(R.string.components_summary_mode_breathing, delaySeconds)
        else -> UiText.Raw(mode)
    }

    private const val NONE = "NONE"

    private val DAY_NAMES: Map<Int, UiText> = mapOf(
        1 to UiText.of(R.string.components_day_short_mon),
        2 to UiText.of(R.string.components_day_short_tue),
        3 to UiText.of(R.string.components_day_short_wed),
        4 to UiText.of(R.string.components_day_short_thu),
        5 to UiText.of(R.string.components_day_short_fri),
        6 to UiText.of(R.string.components_day_short_sat),
        7 to UiText.of(R.string.components_day_short_sun)
    )
}
