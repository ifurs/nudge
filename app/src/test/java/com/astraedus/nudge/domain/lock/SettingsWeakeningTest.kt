package com.astraedus.nudge.domain.lock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which Settings flips the commitment lock challenges (v1.10.0).
 *
 * The asymmetry is the whole point: weakening protection costs a typed challenge, strengthening it
 * is always free, and while Strict Mode is off nothing is gated in either direction. For the escape
 * hatch this replaces the old "frozen + hidden while Strict Mode is on" design — see
 * `EmergencyPassVisibilityTest` for the overlay half of the new contract.
 */
class SettingsWeakeningTest {

    // --- Strict Mode ON: weakening is gated, strengthening is free ---

    @Test
    fun `turning the escape hatch ON under strict mode requires the unlock`() {
        assertTrue(
            SettingsWeakening.requiresUnlock(
                LockedToggle.EMERGENCY_PASS, enable = true, strictModeEnabled = true
            )
        )
    }

    @Test
    fun `turning the escape hatch OFF under strict mode is free`() {
        assertFalse(
            "removing your own bypass strengthens protection — never gate it",
            SettingsWeakening.requiresUnlock(
                LockedToggle.EMERGENCY_PASS, enable = false, strictModeEnabled = true
            )
        )
    }

    @Test
    fun `turning strict mode OFF requires the unlock`() {
        assertTrue(
            SettingsWeakening.requiresUnlock(
                LockedToggle.STRICT_MODE, enable = false, strictModeEnabled = true
            )
        )
    }

    @Test
    fun `turning strict mode ON is free`() {
        assertFalse(
            SettingsWeakening.requiresUnlock(
                LockedToggle.STRICT_MODE, enable = true, strictModeEnabled = false
            )
        )
        // Defensive: even if the screen somehow asks while already on, opting in is never gated.
        assertFalse(
            SettingsWeakening.requiresUnlock(
                LockedToggle.STRICT_MODE, enable = true, strictModeEnabled = true
            )
        )
    }

    @Test
    fun `turning a content filter toggle OFF under strict mode requires the unlock, ON is free`() {
        for (toggle in listOf(LockedToggle.CONTENT_FILTER, LockedToggle.CONTENT_FILTER_STRICT_KEYWORDS)) {
            assertTrue(
                "$toggle OFF weakens protection",
                SettingsWeakening.requiresUnlock(toggle, enable = false, strictModeEnabled = true)
            )
            assertFalse(
                "$toggle ON strengthens protection",
                SettingsWeakening.requiresUnlock(toggle, enable = true, strictModeEnabled = true)
            )
        }
    }

    // --- Strict Mode OFF: nothing is ever gated ---

    @Test
    fun `strict mode off gates nothing in either direction for any toggle`() {
        for (toggle in LockedToggle.entries) {
            for (enable in listOf(true, false)) {
                assertFalse(
                    "$toggle enable=$enable must be free while strict mode is off",
                    SettingsWeakening.requiresUnlock(toggle, enable, strictModeEnabled = false)
                )
            }
        }
    }

    // --- Strict Mode difficulty (challenge length): lowering it weakens, raising it does not ---

    @Test
    fun `lowering the challenge length under strict mode requires the unlock`() {
        assertTrue(
            "Hard -> Easy cuts every later unlock from 48 characters to 12 -- that is weakening",
            SettingsWeakening.requiresUnlockForLength(
                currentLength = StrictModeChallenge.LENGTH_HARD,
                newLength = StrictModeChallenge.LENGTH_EASY,
                strictModeEnabled = true
            )
        )
        assertTrue(
            SettingsWeakening.requiresUnlockForLength(
                currentLength = StrictModeChallenge.LENGTH_HARD,
                newLength = StrictModeChallenge.LENGTH_MEDIUM,
                strictModeEnabled = true
            )
        )
        assertTrue(
            SettingsWeakening.requiresUnlockForLength(
                currentLength = StrictModeChallenge.LENGTH_MEDIUM,
                newLength = StrictModeChallenge.LENGTH_EASY,
                strictModeEnabled = true
            )
        )
    }

    @Test
    fun `raising the challenge length under strict mode is free`() {
        assertFalse(
            SettingsWeakening.requiresUnlockForLength(
                currentLength = StrictModeChallenge.LENGTH_EASY,
                newLength = StrictModeChallenge.LENGTH_HARD,
                strictModeEnabled = true
            )
        )
        assertFalse(
            SettingsWeakening.requiresUnlockForLength(
                currentLength = StrictModeChallenge.LENGTH_MEDIUM,
                newLength = StrictModeChallenge.LENGTH_HARD,
                strictModeEnabled = true
            )
        )
    }

    @Test
    fun `re-selecting the current challenge length is free`() {
        assertFalse(
            SettingsWeakening.requiresUnlockForLength(
                currentLength = StrictModeChallenge.LENGTH_HARD,
                newLength = StrictModeChallenge.LENGTH_HARD,
                strictModeEnabled = true
            )
        )
    }

    @Test
    fun `any challenge length change while strict mode is off is free`() {
        assertFalse(
            SettingsWeakening.requiresUnlockForLength(
                currentLength = StrictModeChallenge.LENGTH_HARD,
                newLength = StrictModeChallenge.LENGTH_EASY,
                strictModeEnabled = false
            )
        )
    }

    @Test
    fun `the shared lowered predicate treats an absent side as nothing to compare`() {
        assertTrue(SettingsWeakening.isChallengeLengthLowered(48, 12))
        assertFalse(SettingsWeakening.isChallengeLengthLowered(12, 48))
        assertFalse(SettingsWeakening.isChallengeLengthLowered(24, 24))
        assertFalse(SettingsWeakening.isChallengeLengthLowered(null, 12))
        assertFalse(SettingsWeakening.isChallengeLengthLowered(48, null))
    }
}
