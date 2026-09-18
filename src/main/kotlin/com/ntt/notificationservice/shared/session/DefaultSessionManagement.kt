package com.ntt.notificationservice.shared.session

import com.ntt.basecore.domain.session.SessionManagement
import org.springframework.stereotype.Component
import java.util.Optional

/**
 * Default SessionManagement implementation for notification-service.
 * Provides auditor identity for JPA entity auditing.
 */
@Component
class DefaultSessionManagement : SessionManagement {

    override fun getSessionUserCurrent(): Optional<String> {
        return Optional.of("SYSTEM")
    }
}
