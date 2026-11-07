package com.astraedus.nudge.ui.screens.config

import com.astraedus.nudge.domain.model.BlockMode

/**
 * Everything the app config screen can ask of its state holder, so [UnifiedAppConfigContent] is a
 * plain `(state, actions)` composable the screenshot gallery can draw without a ViewModel. Every
 * member has a no-op default: a preview or a golden passes `object : UnifiedAppConfigActions {}`.
 * [asActions] is the production adapter, one line per [UnifiedAppConfigViewModel] function.
 */
interface UnifiedAppConfigActions {
    fun setEnabled(enabled: Boolean) {}
    fun setDailyLimitEnabled(enabled: Boolean) {}
    fun setDailyLimitMinutes(minutes: Int) {}
    fun setShowTimeRemaining(show: Boolean) {}
    fun setShowCounter(show: Boolean) {}
    fun setGrayscale(enabled: Boolean) {}
    fun setWebDomainEnabled(enabled: Boolean) {}
    fun setWebDomains(domains: String) {}
    fun setWebBlockMode(mode: BlockMode) {}
    fun setBlocksWholeApp(blocks: Boolean) {}
    fun setDefaultMode(mode: BlockMode) {}
    fun setDefaultDelaySeconds(seconds: Int) {}
    fun setKeepPassOnShortTrips(enabled: Boolean) {}
    fun setDefaultAutoKickEnabled(enabled: Boolean) {}
    fun setDefaultAutoKickByInteractions(enabled: Boolean) {}
    fun setDefaultAutoKickAfter(count: Int) {}
    fun setDefaultAutoKickAfterMinutesText(text: String) {}
    fun setDefaultAutoKickCooldownMinutesText(text: String) {}
    fun setReminderEnabled(enabled: Boolean) {}
    fun setRemindAfterMinutesText(text: String) {}
    fun setFeatureOverride(featureKey: String, override: FeatureOverride) {}
    fun setTabVanish(enabled: Boolean) {}
    fun setFollowingSteer(enabled: Boolean) {}
    fun setScheduledOverrideEnabled(enabled: Boolean) {}
    fun setScheduledOverrideFirstUnlock(firstUnlock: Boolean) {}
    fun setScheduleDays(days: Set<Int>) {}
    fun setScheduleStartTime(hour: Int, minute: Int) {}
    fun setScheduleEndTime(hour: Int, minute: Int) {}
    fun setScheduledMode(mode: BlockMode) {}
    fun setScheduledDelaySeconds(seconds: Int) {}
    fun setScheduledFeatureOverride(featureKey: String, override: FeatureOverride) {}
    fun setFirstUnlockMinutes(minutes: Int) {}
    fun setDayStartMinute(minute: Int) {}
    fun save() {}
    fun showDeleteConfirmation() {}
    fun dismissDeleteConfirmation() {}
    fun deleteAllRules() {}
}

fun UnifiedAppConfigViewModel.asActions(): UnifiedAppConfigActions {
    val viewModel = this
    return object : UnifiedAppConfigActions {
        override fun setEnabled(enabled: Boolean) = viewModel.setEnabled(enabled)
        override fun setDailyLimitEnabled(enabled: Boolean) = viewModel.setDailyLimitEnabled(enabled)
        override fun setDailyLimitMinutes(minutes: Int) = viewModel.setDailyLimitMinutes(minutes)
        override fun setShowTimeRemaining(show: Boolean) = viewModel.setShowTimeRemaining(show)
        override fun setShowCounter(show: Boolean) = viewModel.setShowCounter(show)
        override fun setGrayscale(enabled: Boolean) = viewModel.setGrayscale(enabled)
        override fun setWebDomainEnabled(enabled: Boolean) = viewModel.setWebDomainEnabled(enabled)
        override fun setWebDomains(domains: String) = viewModel.setWebDomains(domains)
        override fun setWebBlockMode(mode: BlockMode) = viewModel.setWebBlockMode(mode)
        override fun setBlocksWholeApp(blocks: Boolean) = viewModel.setBlocksWholeApp(blocks)
        override fun setDefaultMode(mode: BlockMode) = viewModel.setDefaultMode(mode)
        override fun setDefaultDelaySeconds(seconds: Int) = viewModel.setDefaultDelaySeconds(seconds)
        override fun setKeepPassOnShortTrips(enabled: Boolean) = viewModel.setKeepPassOnShortTrips(enabled)
        override fun setDefaultAutoKickEnabled(enabled: Boolean) = viewModel.setDefaultAutoKickEnabled(enabled)
        override fun setDefaultAutoKickByInteractions(enabled: Boolean) = viewModel.setDefaultAutoKickByInteractions(enabled)
        override fun setDefaultAutoKickAfter(count: Int) = viewModel.setDefaultAutoKickAfter(count)
        override fun setDefaultAutoKickAfterMinutesText(text: String) = viewModel.setDefaultAutoKickAfterMinutesText(text)
        override fun setDefaultAutoKickCooldownMinutesText(text: String) = viewModel.setDefaultAutoKickCooldownMinutesText(text)
        override fun setReminderEnabled(enabled: Boolean) = viewModel.setReminderEnabled(enabled)
        override fun setRemindAfterMinutesText(text: String) = viewModel.setRemindAfterMinutesText(text)
        override fun setFeatureOverride(featureKey: String, override: FeatureOverride) = viewModel.setFeatureOverride(featureKey, override)
        override fun setTabVanish(enabled: Boolean) = viewModel.setTabVanish(enabled)
        override fun setFollowingSteer(enabled: Boolean) = viewModel.setFollowingSteer(enabled)
        override fun setScheduledOverrideEnabled(enabled: Boolean) = viewModel.setScheduledOverrideEnabled(enabled)
        override fun setScheduledOverrideFirstUnlock(firstUnlock: Boolean) = viewModel.setScheduledOverrideFirstUnlock(firstUnlock)
        override fun setScheduleDays(days: Set<Int>) = viewModel.setScheduleDays(days)
        override fun setScheduleStartTime(hour: Int, minute: Int) = viewModel.setScheduleStartTime(hour, minute)
        override fun setScheduleEndTime(hour: Int, minute: Int) = viewModel.setScheduleEndTime(hour, minute)
        override fun setScheduledMode(mode: BlockMode) = viewModel.setScheduledMode(mode)
        override fun setScheduledDelaySeconds(seconds: Int) = viewModel.setScheduledDelaySeconds(seconds)
        override fun setScheduledFeatureOverride(featureKey: String, override: FeatureOverride) = viewModel.setScheduledFeatureOverride(featureKey, override)
        override fun setFirstUnlockMinutes(minutes: Int) = viewModel.setFirstUnlockMinutes(minutes)
        override fun setDayStartMinute(minute: Int) = viewModel.setDayStartMinute(minute)
        override fun save() = viewModel.save()
        override fun showDeleteConfirmation() = viewModel.showDeleteConfirmation()
        override fun dismissDeleteConfirmation() = viewModel.dismissDeleteConfirmation()
        override fun deleteAllRules() = viewModel.deleteAllRules()
    }
}
