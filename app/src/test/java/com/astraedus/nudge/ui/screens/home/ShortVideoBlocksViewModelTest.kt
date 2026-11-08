package com.astraedus.nudge.ui.screens.home

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.astraedus.nudge.MainDispatcherRule
import com.astraedus.nudge.data.db.entity.BlockRule
import com.astraedus.nudge.data.repository.BlockRuleRepository
import com.astraedus.nudge.domain.model.BlockMode
import com.astraedus.nudge.domain.model.FeatureMode
import com.astraedus.nudge.domain.packages.FakeEssentialPackages
import com.astraedus.nudge.domain.packages.FakePackageFacts
import com.astraedus.nudge.domain.usecase.SetDayStartMinuteUseCase
import com.astraedus.nudge.testing.FakeInstalledAppsRepository
import com.astraedus.nudge.testing.FakeNudgePreferences
import com.astraedus.nudge.testing.FakeRuleDatabase
import com.astraedus.nudge.testing.aBlockRule
import com.astraedus.nudge.testing.anApp
import com.astraedus.nudge.ui.screens.config.UnifiedAppConfigViewModel
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The Home "Block short videos" switches, driven through the REAL view model, repository and
 * Strict Mode gate over an in-memory database. Pins the self-control contract: switching ON is free,
 * switching OFF takes the typed unlock challenge while Strict Mode is on, and what a switch writes is
 * what the app config screen reads back.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ShortVideoBlocksViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(dispatcher)

    private val instagram = "com.instagram.android"
    private val db = FakeRuleDatabase()
    private val accessibility = MutableStateFlow(true)

    private fun viewModel(
        strictOn: Boolean = false,
        installed: Boolean = true,
        repository: BlockRuleRepository = db.repository
    ): ShortVideoBlocksViewModel {
        val apps = if (installed) arrayOf(anApp(instagram, "Instagram")) else emptyArray()
        return ShortVideoBlocksViewModel(
            context = mockk<Context>(relaxed = true),
            blockRuleRepository = repository,
            installedAppsRepository = FakeInstalledAppsRepository(*apps),
            nudgePreferences = FakeNudgePreferences { setStrictModeEnabled(strictOn) },
            accessibilityHealth = object : AccessibilityHealth(mockk(relaxed = true)) {
                override val working = accessibility
            },
            ioDispatcher = dispatcher
        )
    }

    /** Keeps the (lazily shared) state hot for the test, then returns the Instagram Reels row. */
    private fun TestScope.reelsRow(vm: ShortVideoBlocksViewModel): ShortVideoRow {
        backgroundScope.launch(dispatcher) { vm.uiState.collect { } }
        return vm.uiState.value.rows.first { it.target.kind == ShortVideoKind.REELS }
    }

    private fun featureRules(): List<BlockRule> = db.rules.filter { it.inAppFeatures == "REELS" }

    @Test
    fun `no installed app - no rows, and the card knows it has loaded`() = runTest(dispatcher) {
        val vm = viewModel(installed = false)
        backgroundScope.launch(dispatcher) { vm.uiState.collect { } }
        assertTrue(vm.uiState.value.loaded)
        assertTrue(vm.uiState.value.rows.isEmpty())
    }

    @Test
    fun `switching ON writes a Hard Block for Reels and leaves the app itself open`() = runTest(dispatcher) {
        val vm = viewModel(strictOn = true) // ON is free even under Strict Mode
        val row = reelsRow(vm)

        vm.setBlocked(row, true)

        assertNull("no challenge for a stronger rule", vm.challenge.value)
        assertEquals(BlockMode.HARD_BLOCK.name, featureRules().single().mode)
        val appRule = db.rules.single { it.inAppFeatures == null }
        assertEquals(BlockMode.NONE.name, appRule.mode)
        assertEquals(ShortVideoStatus.BLOCKED, reelsRow(vm).status)
    }

    @Test
    fun `what a switch writes is what the config screen shows as Block, with the app open`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.setBlocked(reelsRow(vm), true)

        val config = UnifiedAppConfigViewModel(
            SavedStateHandle(mapOf("packageName" to instagram)),
            db.repository,
            FakeInstalledAppsRepository(anApp(instagram, "Instagram")),
            FakeEssentialPackages(),
            FakePackageFacts(),
            FakeNudgePreferences(),
            SetDayStartMinuteUseCase(FakeNudgePreferences(), object : com.astraedus.nudge.data.repository.RuleSnapshots {
                override suspend fun current() =
                    com.astraedus.nudge.data.repository.RuleSnapshot.of(db.rules, emptyList(), emptyList())
                override suspend fun enabledRules() = db.rules
            }),
            mockk<Context>(relaxed = true)
        )

        val state = config.uiState.first { !it.isLoading }
        assertEquals(FeatureMode.BLOCK, state.featureOverrides.getValue("REELS").mode)
        assertFalse("whole-app blocking stays off", state.blocksWholeApp)
    }

    @Test
    fun `strict off - switching OFF removes the block at once`() = runTest(dispatcher) {
        val vm = viewModel(strictOn = false)
        vm.setBlocked(reelsRow(vm), true)

        vm.setBlocked(reelsRow(vm), false)

        assertNull(vm.challenge.value)
        assertTrue(featureRules().isEmpty())
        assertTrue("the bare app row goes with it", db.rules.isEmpty())
    }

    @Test
    fun `strict on - switching OFF waits for the challenge and removes nothing`() = runTest(dispatcher) {
        val vm = viewModel(strictOn = true)
        vm.setBlocked(reelsRow(vm), true)
        val before = db.rules

        vm.setBlocked(reelsRow(vm), false)

        assertNotNull(vm.challenge.value)
        assertEquals(before, db.rules)
    }

    @Test
    fun `strict on - the right answer removes the block, a wrong answer or cancel keeps it`() = runTest(dispatcher) {
        val vm = viewModel(strictOn = true)
        vm.setBlocked(reelsRow(vm), true)
        vm.setBlocked(reelsRow(vm), false)

        vm.verifyChallenge("not-the-code")
        assertEquals(1, featureRules().size)

        vm.cancelChallenge()
        assertNull(vm.challenge.value)
        assertEquals(1, featureRules().size)

        vm.setBlocked(reelsRow(vm), false)
        vm.verifyChallenge(vm.challenge.value!!.target)
        assertTrue(featureRules().isEmpty())
        assertNull(vm.challenge.value)
    }

    @Test
    fun `strict on - a block that was already off needs no challenge to remove`() = runTest(dispatcher) {
        db.seedRule(aBlockRule(packageName = instagram, mode = "NONE"))
        db.seedRule(aBlockRule(packageName = instagram, mode = "HARD_BLOCK", enabled = false, inAppFeatures = "REELS"))
        val vm = viewModel(strictOn = true)

        vm.setBlocked(reelsRow(vm), false)

        assertNull(vm.challenge.value)
        assertTrue(featureRules().isEmpty())
    }

    /**
     * CWE-367. The switch-OFF is tapped while a switch-ON for the same row holds the write lock and
     * has not written yet: the rule is still disabled, so a read taken at tap time says "removing
     * it weakens nothing". By the time the OFF gets the lock the ON has enabled the rule, and the
     * removal weakens. That has to be judged from the rules under the lock, not from the tap-time
     * read: the OFF is challenged and nothing is deleted before the answer.
     */
    @Test
    fun `strict on - a switch-ON that lands between tap and write makes the OFF a challenged weakening`() =
        runTest(dispatcher) {
            db.seedRule(aBlockRule(packageName = instagram, mode = "NONE"))
            db.seedRule(
                aBlockRule(packageName = instagram, mode = "HARD_BLOCK", enabled = false, inAppFeatures = "REELS")
            )
            val onWritesGate = CompletableDeferred<Unit>()
            val repository = spyk(db.repository)
            coEvery { repository.updateRule(any()) } coAnswers {
                onWritesGate.await() // the ON holds the lock here, the rule still disabled in the table
                callOriginal()
            }
            val vm = viewModel(strictOn = true, repository = repository)
            val row = reelsRow(vm)

            vm.setBlocked(row, true) // takes the lock, parks inside its write
            vm.setBlocked(row, false) // tapped now: the rule reads as disabled, OFF looks free
            assertFalse("the ON has not written yet", featureRules().single().enabled)

            onWritesGate.complete(Unit) // the ON lands; the OFF now gets the lock

            assertNotNull("the OFF is a weakening now and must be challenged", vm.challenge.value)
            assertTrue("nothing is deleted before the answer", featureRules().single().enabled)

            vm.verifyChallenge("not-the-code")
            assertEquals(1, featureRules().size)

            vm.verifyChallenge(vm.challenge.value!!.target)
            assertTrue(featureRules().isEmpty())
            assertNull(vm.challenge.value)
        }

    @Test
    fun `strict on - cancelling the challenge raised by the in-lock check writes nothing`() =
        runTest(dispatcher) {
            db.seedRule(aBlockRule(packageName = instagram, mode = "NONE"))
            db.seedRule(
                aBlockRule(packageName = instagram, mode = "HARD_BLOCK", enabled = false, inAppFeatures = "REELS")
            )
            val onWritesGate = CompletableDeferred<Unit>()
            val repository = spyk(db.repository)
            coEvery { repository.updateRule(any()) } coAnswers {
                onWritesGate.await()
                callOriginal()
            }
            val vm = viewModel(strictOn = true, repository = repository)
            val row = reelsRow(vm)
            vm.setBlocked(row, true)
            vm.setBlocked(row, false)
            onWritesGate.complete(Unit)
            val before = db.rules

            vm.cancelChallenge()

            assertNull(vm.challenge.value)
            assertEquals(before, db.rules)
            assertTrue(featureRules().single().enabled)
        }

    @Test
    fun `the accessibility state reaches the card`() = runTest(dispatcher) {
        val vm = viewModel()
        backgroundScope.launch(dispatcher) { vm.uiState.collect { } }
        assertTrue(vm.uiState.value.accessibilityWorking)
        accessibility.value = false
        assertFalse(vm.uiState.value.accessibilityWorking)
    }
}
