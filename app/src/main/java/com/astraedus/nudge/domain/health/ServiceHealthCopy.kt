package com.astraedus.nudge.domain.health

import com.astraedus.nudge.domain.text.UiText

/**
 * The one place a [ServiceHealth] becomes words the user reads (`ServiceHealth.notificationCopy()` in
 * `service/ServiceHealthText.kt` fills it with resource ids; the domain holds only the shape).
 *
 * Separate from the notification code because the defect this subsystem exists to fix was a
 * *sentence* ("Nudge is active" while Nudge was not active), not a mechanism, and a sentence that
 * lives inside an Android class is a sentence no JVM test can read. Every state is spelled out here
 * with no `else` branch, so adding a state to [ServiceHealth] fails to compile rather than falling
 * into a default that says the reassuring thing.
 */
data class ServiceHealthCopy(val title: UiText, val body: UiText)

