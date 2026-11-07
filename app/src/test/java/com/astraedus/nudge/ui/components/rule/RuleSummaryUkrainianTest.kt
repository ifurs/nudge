package com.astraedus.nudge.ui.components.rule

import com.astraedus.nudge.domain.model.RuleLabel
import com.astraedus.nudge.testing.aBlockRule
import com.astraedus.nudge.testing.uk
import com.astraedus.nudge.ui.overlay.toUiText
import com.astraedus.nudge.ui.screens.stats.StatsDateLabels
import com.astraedus.nudge.ui.text.DurationText
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The Ukrainian reading of the shared summary lines, labels and durations: the same inputs as the
 * English tests, so a translation that loses a placeholder, a plural form or a word order fails
 * here and not on a phone.
 */
class RuleSummaryUkrainianTest {

    private val appRule = aBlockRule(packageName = "com.test")

    @Test
    fun `a rule line reads mode, schedule and limit in Ukrainian`() {
        assertEquals(
            "Затримка 15 с · Ліміт 30 хв",
            RuleSummary.buildSummaryText(listOf(appRule.copy(mode = "DELAY", delaySeconds = 15, dailyLimitMinutes = 30))).uk()
        )
        assertEquals(
            "Повне блокування Пн–Пт 9:00–17:00",
            RuleSummary.buildSummaryText(
                listOf(
                    appRule.copy(
                        mode = "HARD_BLOCK", scheduleDays = "1,2,3,4,5",
                        scheduleStartMinute = 9 * 60, scheduleEndMinute = 17 * 60
                    )
                )
            ).uk()
        )
        assertEquals(
            "Затримка 15 с · Reels: Повне блокування",
            RuleSummary.buildSummaryText(
                listOf(
                    appRule.copy(mode = "DELAY", delaySeconds = 15),
                    appRule.copy(mode = "HARD_BLOCK", inAppFeatures = "REELS")
                )
            ).uk()
        )
    }

    @Test
    fun `a group title takes the right ukrainian plural form`() {
        assertEquals("Робота · 1 застосунок", RuleSummary.groupTitle("Робота", 1).uk())
        assertEquals("Робота · 3 застосунки", RuleSummary.groupTitle("Робота", 3).uk())
        assertEquals("Робота · 9 застосунків", RuleSummary.groupTitle("Робота", 9).uk())
        assertEquals("Робота · 21 застосунок", RuleSummary.groupTitle("Робота", 21).uk())
    }

    @Test
    fun `an All apps title and the covering lines read in Ukrainian`() {
        assertEquals("Усі застосунки", RuleSummary.allAppsTitle(emptyList()).uk())
        assertEquals("Усі застосунки, крім Spotify", RuleSummary.allAppsTitle(listOf("Spotify")).uk())
        assertEquals("Усі застосунки, крім A, B і ще 1", RuleSummary.allAppsTitle(listOf("A", "B", "C")).uk())
        assertEquals("У групі: Соцмережі", RuleSummary.coveredByText(listOf("Соцмережі"))!!.uk())
        assertEquals("У групах: A, B", RuleSummary.coveredByText(listOf("A", "B"))!!.uk())
        assertEquals("У 3 групах", RuleSummary.coveredByText(listOf("A", "B", "C"))!!.uk())
    }

    @Test
    fun `a first-unlock window reads in Ukrainian`() {
        assertEquals("30 хв після першого розблокування", RuleSummary.firstUnlockText(null, 30).uk())
        assertEquals("1 год після першого розблокування (Пн–Пт)", RuleSummary.firstUnlockText(listOf(1, 2, 3, 4, 5), 60).uk())
    }

    @Test
    fun `durations and dates carry ukrainian unit words`() {
        assertEquals("2\u00A0год 5\u00A0хв", DurationText.of((2 * 60 + 5) * 60_000L).uk())
        assertEquals("1\u00A0хв 30\u00A0с", DurationText.of(90_000L).uk())
        assertEquals("<\u00A01\u00A0хв", DurationText.dayTotal(5_000L).uk())
        assertEquals("Сьогодні", StatsDateLabels.day(LocalDate.of(2026, 8, 27), LocalDate.of(2026, 8, 27)).uk())
        assertEquals("Учора", StatsDateLabels.day(LocalDate.of(2026, 8, 26), LocalDate.of(2026, 8, 27)).uk())
    }

    @Test
    fun `the rule label on the block screen reads in Ukrainian`() {
        val label = RuleLabel(
            subject = RuleLabel.Subject.Group("Соцмережі"),
            scheduled = true,
            dailyLimitMinutes = 30
        )
        assertEquals("Соцмережі (за розкладом, 30 хв/день)", label.toUiText().uk())
        assertEquals("Соцмережі (за розкладом, 30 хв/день) (ліміт вичерпано)", label.withLimitReached().toUiText().uk())
        assertEquals("Ядерний режим", RuleLabel.of(RuleLabel.Subject.Nuke).toUiText().uk())
    }
}
