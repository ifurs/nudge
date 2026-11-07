package com.astraedus.nudge.domain.lock

import com.astraedus.nudge.testing.aBlockRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** L1: which feature-override edits App config's save must put behind the Strict Mode challenge. */
class FeatureOverrideWeakeningTest {

    private fun reels(mode: String = "HARD_BLOCK", enabled: Boolean = true, delay: Int = 15) = aBlockRule(
        packageName = "com.instagram.android",
        inAppFeatures = "REELS",
        mode = mode,
        enabled = enabled,
        delaySeconds = delay
    )

    @Test
    fun `an override that is no longer written is a weakening - Hard Block back to Inherit`() {
        assertTrue(FeatureOverrideWeakening.isWeakened(old = mapOf("REELS" to reels()), new = emptyMap()))
    }

    @Test
    fun `dropping an override that was already off is not a weakening`() {
        assertFalse(
            FeatureOverrideWeakening.isWeakened(old = mapOf("REELS" to reels(enabled = false)), new = emptyMap())
        )
    }

    @Test
    fun `softening an override that stays is a weakening`() {
        assertTrue(FeatureOverrideWeakening.isWeakened(mapOf("REELS" to reels()), mapOf("REELS" to reels("DELAY"))))
        assertTrue(
            FeatureOverrideWeakening.isWeakened(
                mapOf("REELS" to reels("DELAY", delay = 30)),
                mapOf("REELS" to reels("DELAY", delay = 5))
            )
        )
        assertTrue(
            FeatureOverrideWeakening.isWeakened(mapOf("REELS" to reels()), mapOf("REELS" to reels(enabled = false)))
        )
    }

    @Test
    fun `strengthening, adding and leaving alone are free`() {
        assertFalse(FeatureOverrideWeakening.isWeakened(emptyMap(), mapOf("REELS" to reels())))
        assertFalse(FeatureOverrideWeakening.isWeakened(mapOf("REELS" to reels("DELAY")), mapOf("REELS" to reels())))
        assertFalse(FeatureOverrideWeakening.isWeakened(mapOf("REELS" to reels()), mapOf("REELS" to reels())))
        assertFalse(FeatureOverrideWeakening.isWeakened(emptyMap(), emptyMap()))
    }

    @Test
    fun `one feature kept does not hide another one dropped`() {
        val old = mapOf("REELS" to reels(), "EXPLORE" to reels().copy(inAppFeatures = "EXPLORE"))
        val new = mapOf("REELS" to reels())

        assertTrue(FeatureOverrideWeakening.isWeakened(old, new))
    }
}
