package com.astraedus.nudge.domain.autokick

import com.astraedus.nudge.ui.text.DurationText
import com.astraedus.nudge.service.bodyText
import com.astraedus.nudge.service.titleText
import com.astraedus.nudge.testing.uk
import com.astraedus.nudge.testing.en
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class UseReminderTest {

    @Test
    fun `the title names the app and the configured minutes`() {
        assertEquals("You've been on Instagram for 20 min", UseReminder("Instagram", 20).titleText().en())
    }

    @Test
    fun `the body is the gentle question`() {
        assertEquals("Just a heads-up. Still what you came here for?", UseReminder("YouTube", 5).bodyText().en())
    }

    @Test
    fun `durations of an hour or more read as hours`() {
        assertEquals("1 min", DurationText.spoken(1).en())
        assertEquals("59 min", DurationText.spoken(59).en())
        assertEquals("1 hr", DurationText.spoken(60).en())
        assertEquals("1 hr 30 min", DurationText.spoken(90).en())
        assertEquals("2 hr", DurationText.spoken(120).en())
        assertEquals("24 hr", DurationText.spoken(1440).en())
    }

    @Test
    fun `a nonsense duration never renders zero`() {
        assertEquals("1 min", DurationText.spoken(0).en())
        assertEquals("1 min", DurationText.spoken(-3).en())
    }

    @Test
    fun `the copy never scolds`() {
        // Nudge's voice (NudgeMessages): a fact the user asked for, then one open question.
        val banned = listOf("wasted", "too long", "again", "addict", "should", "!")
        for (minutes in listOf(1, 20, 60, 90, 1440)) {
            val reminder = UseReminder("TikTok", minutes)
            val text = (reminder.titleText().en() + " " + reminder.bodyText().en()).lowercase()
            for (word in banned) assertFalse("'$word' in: $text", word in text)
        }
    }

    @Test
    fun `the ukrainian reminder reads in the same shape`() {
        val reminder = UseReminder("Instagram", 90)
        assertEquals("Ви вже 1 год 30 хв у Instagram", reminder.titleText().uk())
        assertEquals("Просто нагадування. Це досі те, заради чого ви прийшли?", reminder.bodyText().uk())
    }
}
