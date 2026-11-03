package com.astraedus.nudge.ui.screens.settings

import com.astraedus.nudge.testing.SourceScan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Source-level guard for "lowering the Strict Mode difficulty is a weakening action".
 *
 * The bug: the Easy / Medium / Hard chips called `setStrictModeChallengeLength` directly, so Hard
 * (48 characters) -> Easy (12) while Strict Mode was on made every later unlock cheaper with no
 * challenge at all -- the same change `ImportedSettingsWeakening` already gates on the import path.
 * The decision itself is unit-tested (`SettingsWeakeningTest`); what only the SHAPE of the code can
 * lose is that the write goes through that decision, so this pins it, the way
 * `BackupImportGateTest` pins the import write.
 */
class StrictModeDifficultyGateContractTest {

    /** Production code with comments stripped, so documenting the bug cannot trip the scan. */
    private fun code(file: java.io.File): String =
        file.readText()
            .replace(Regex("""/\*[\s\S]*?\*/"""), "")
            .lines()
            .joinToString("\n") { it.substringBefore("//") }

    @Test
    fun `the UI writes the challenge length from exactly one place, and it asks the policy first`() {
        val callers = SourceScan.kotlinFile("").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { code(it).contains(".setStrictModeChallengeLength(") }
            .map { it.name }
            .toList()

        assertEquals(
            "setStrictModeChallengeLength must have no ungated UI call site: Settings is the only caller",
            listOf("SettingsScreen.kt"),
            callers
        )

        val settings = code(SourceScan.kotlinFile("com/astraedus/nudge/ui/screens/settings/SettingsScreen.kt"))
        assertEquals(
            "all three difficulty chips must share ONE write, so there is one place to gate",
            1,
            Regex("""\.setStrictModeChallengeLength\(""").findAll(settings).count()
        )
        // The write lives in the handler the chips share, in the same block as the policy decision
        // (the write is a lambda defined above the `if`, so text order says nothing; the block does).
        val handler = settings.substringAfter("val onDifficultySelect", "").substringBefore("Row(")
        assertTrue(
            "the chips' shared handler must exist and contain the write",
            handler.contains(".setStrictModeChallengeLength(")
        )
        assertTrue(
            "the same handler must ask SettingsWeakening.requiresUnlockForLength before the write applies",
            handler.contains("SettingsWeakening.requiresUnlockForLength(")
        )
        assertEquals(
            "all three chips must route through that handler",
            3,
            Regex("""onSelect\s*=\s*onDifficultySelect""").findAll(settings).count()
        )
    }
}
