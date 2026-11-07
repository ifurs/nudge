package com.astraedus.nudge.service

import com.astraedus.nudge.R
import com.astraedus.nudge.domain.health.ServiceHealth
import com.astraedus.nudge.domain.health.ServiceHealthCopy
import com.astraedus.nudge.domain.text.UiText

/**
 * Copy for the ongoing foreground-service notification.
 *
 * Every state is spelled out with no `else` branch, so adding a state to [ServiceHealth] fails to
 * compile rather than falling into a default that says the reassuring thing (the defect this
 * subsystem exists to fix was a *sentence*: "Nudge is active" while Nudge was not active).
 *
 * [ServiceHealth.DISABLED] has copy even though the service stops itself in that state: the value
 * has to exist for the exhaustiveness test to mean anything, and a stop is not instantaneous.
 */
fun ServiceHealth.notificationCopy(): ServiceHealthCopy = when (this) {
    ServiceHealth.DISABLED -> ServiceHealthCopy(
        title = UiText.of(R.string.service_health_disabled_title),
        body = UiText.of(R.string.service_health_disabled_body)
    )
    ServiceHealth.PERMISSION_MISSING -> ServiceHealthCopy(
        title = UiText.of(R.string.service_health_not_blocking_title),
        body = UiText.of(R.string.service_health_permission_body)
    )
    ServiceHealth.STOPPED_BY_SYSTEM -> ServiceHealthCopy(
        title = UiText.of(R.string.service_health_not_blocking_title),
        body = UiText.of(R.string.service_health_stopped_body)
    )
    ServiceHealth.ACTIVE -> ServiceHealthCopy(
        title = UiText.of(R.string.service_health_active_title),
        body = UiText.of(R.string.service_health_active_body)
    )
}
