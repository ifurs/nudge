package com.astraedus.nudge.service

import com.astraedus.nudge.ui.text.DurationText
import com.astraedus.nudge.testing.en
import org.junit.Assert.assertEquals
import org.junit.Test

class CounterOverlayManagerTest {

    private fun compact(ms: Long): String = DurationText.compact(ms).en()

    @Test
    fun `formatCompactDuration formats 0 as 0s`() {
        assertEquals("0s", compact(0L))
    }

    @Test
    fun `formatCompactDuration formats seconds only`() {
        assertEquals("45s", compact(45_000L))
    }

    @Test
    fun `formatCompactDuration formats minutes only`() {
        assertEquals("42m", compact(42L * 60 * 1000))
    }

    @Test
    fun `formatCompactDuration formats hours and minutes`() {
        assertEquals("1h 12m", compact((1L * 60 + 12) * 60 * 1000))
    }

    @Test
    fun `formatCompactDuration formats exactly 1 hour as 1h 0m`() {
        assertEquals("1h 0m", compact(3_600_000L))
    }

    @Test
    fun `formatCompactDuration handles negative values`() {
        assertEquals("0s", compact(-1000L))
    }

    @Test
    fun `formatCompactDuration rounds down sub-minute to minutes`() {
        // 2 minutes and 30 seconds -> should show 2m (no seconds when minutes > 0)
        assertEquals("2m", compact(150_000L))
    }
}
