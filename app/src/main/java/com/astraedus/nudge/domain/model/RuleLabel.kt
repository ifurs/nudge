package com.astraedus.nudge.domain.model

/**
 * What a block screen's "Rule: ..." line names, as DATA. The engine decides WHAT the label is about
 * (a group, a mode, a website rule, Nuke...) and the UI layer turns it into words in the user's
 * language (`RuleLabelText` in `ui/overlay`), so no English is built in `domain/`.
 *
 * English reading, unchanged: "Reels - Delay", "Hard Block (30 min/day)", "Social (scheduled)",
 * "Web - Hard Block", "Social (30 min/day) (limit reached)".
 *
 * @property features the in-app features the rule is about ("reels", "shorts"), as stored.
 * @property scheduled the rule carries days, so it reads "(scheduled)".
 * @property dailyLimitMinutes the rule's daily cap, which reads "(30 min/day)".
 * @property limitReached the block is the daily-limit hard block, which reads "(limit reached)".
 */
data class RuleLabel(
    val subject: Subject,
    val features: List<String> = emptyList(),
    val scheduled: Boolean = false,
    val dailyLimitMinutes: Int? = null,
    val limitReached: Boolean = false
) {

    /** The main word of the label. */
    sealed interface Subject {
        /** An app rule, named by its mode. [NONE][BlockMode.NONE] reads "Daily limit" or "No block". */
        data class Mode(val mode: BlockMode, val capsUsage: Boolean) : Subject

        /** A group rule, named after its group; a null name reads as "Group". */
        data class Group(val name: String?) : Subject

        data object AllApps : Subject

        /** A website rule: "Web - <mode>". */
        data class Web(val mode: BlockMode) : Subject

        data object ContentFilter : Subject

        data object Nuke : Subject

        /** Text that is data, not copy (a name the user typed). */
        data class Named(val text: String) : Subject
    }

    /** The same label with "(limit reached)" appended. */
    fun withLimitReached(): RuleLabel = copy(limitReached = true)

    companion object {
        fun of(subject: Subject) = RuleLabel(subject)

        /** A label that is just [text]. */
        fun named(text: String) = RuleLabel(Subject.Named(text))
    }
}
