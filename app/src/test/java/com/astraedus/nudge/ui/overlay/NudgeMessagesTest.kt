package com.astraedus.nudge.ui.overlay

import com.astraedus.nudge.testing.XmlTextResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NudgeMessagesTest {

    private val defaults = listOf("Default A", "Default B")

    @Test
    fun `message lists are non-empty`() {
        for (resolver in listOf(XmlTextResolver.english, XmlTextResolver.ukrainian)) {
            assertFalse(resolver.stringArray(NudgeMessages.DELAY_TITLES).isEmpty())
            assertFalse(resolver.stringArray(NudgeMessages.DELAY_SUBTITLES).isEmpty())
            assertFalse(resolver.stringArray(NudgeMessages.HARD_BLOCK_MESSAGES).isEmpty())
        }
    }

    @Test
    fun `both languages ship the same number of built-in messages`() {
        for (array in listOf(
            NudgeMessages.DELAY_TITLES,
            NudgeMessages.DELAY_SUBTITLES,
            NudgeMessages.HARD_BLOCK_MESSAGES
        )) {
            assertEquals(
                XmlTextResolver.english.stringArray(array).size,
                XmlTextResolver.ukrainian.stringArray(array).size
            )
        }
    }

    @Test
    fun `resolvePool falls back to defaults when custom is null`() {
        assertEquals(defaults, NudgeMessages.resolvePool(null, defaults))
    }

    @Test
    fun `resolvePool falls back to defaults when custom is empty string`() {
        assertEquals(defaults, NudgeMessages.resolvePool("", defaults))
    }

    @Test
    fun `resolvePool falls back to defaults when custom is blank`() {
        assertEquals(defaults, NudgeMessages.resolvePool("   ", defaults))
    }

    @Test
    fun `resolvePool falls back to defaults when custom is only newlines and whitespace`() {
        assertEquals(defaults, NudgeMessages.resolvePool("\n  \n\t\n", defaults))
    }

    @Test
    fun `resolvePool parses multiline custom text into trimmed lines`() {
        val custom = "First message\nSecond message\nThird message"
        assertEquals(
            listOf("First message", "Second message", "Third message"),
            NudgeMessages.resolvePool(custom, defaults)
        )
    }

    @Test
    fun `resolvePool handles a single line`() {
        assertEquals(listOf("Only one"), NudgeMessages.resolvePool("Only one", defaults))
    }

    @Test
    fun `resolvePool trims surrounding whitespace on each line`() {
        val custom = "  leading\ntrailing  \n  both  "
        assertEquals(
            listOf("leading", "trailing", "both"),
            NudgeMessages.resolvePool(custom, defaults)
        )
    }

    @Test
    fun `resolvePool drops blank lines mixed among real ones`() {
        val custom = "First\n\n   \nSecond\n\t\nThird\n"
        assertEquals(
            listOf("First", "Second", "Third"),
            NudgeMessages.resolvePool(custom, defaults)
        )
    }
}
