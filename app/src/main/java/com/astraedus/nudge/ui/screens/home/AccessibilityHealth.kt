package com.astraedus.nudge.ui.screens.home

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import com.astraedus.nudge.service.AccessibilityConnectionSignal
import com.astraedus.nudge.service.ProtectionStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Whether Nudge's accessibility service is bound and alive, as a live flow, for the screens that
 * must say "Nudge can't see Reels yet" (the Home card, the onboarding step).
 *
 * The answer is [ProtectionStatus.isAccessibilityServiceWorking] (granted AND connected), never the
 * settings string alone, for the reasons written up on [ProtectionStatus]. It is re-read when the
 * system flips the setting (a ContentObserver, which fires while the user is still in the system
 * screen in a split window) and when the service itself binds or tears down
 * ([AccessibilityConnectionSignal]), the same two triggers the Settings screen's tick uses.
 *
 * Open so a JVM test can stand in for the platform; the platform reads are in [read].
 */
open class AccessibilityHealth @Inject constructor(
    @ApplicationContext private val context: Context
) {

    protected open fun read(): Boolean = ProtectionStatus.isAccessibilityServiceWorking(context)

    open val working: Flow<Boolean> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(read())
            }
        }
        context.contentResolver.registerContentObserver(
            ProtectionStatus.ACCESSIBILITY_SERVICES_URI,
            /* notifyForDescendants = */ false,
            observer
        )
        // A StateFlow delivers its current value first, so this also sends the initial reading and
        // picks up a bind that happened before the observer was registered.
        val signal = launch {
            AccessibilityConnectionSignal.generation.collect { trySend(read()) }
        }
        awaitClose {
            context.contentResolver.unregisterContentObserver(observer)
            signal.cancel()
        }
    }.distinctUntilChanged()
}
