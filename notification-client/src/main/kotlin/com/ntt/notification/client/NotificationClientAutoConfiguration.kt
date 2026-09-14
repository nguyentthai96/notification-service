package com.ntt.notification.client

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean

/**
 * Auto-configuration for notification-client.
 * Activated when `app.notification.client.enabled=true` (default: true).
 */
@AutoConfiguration
@ConditionalOnProperty(
    prefix = "app.notification.client",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class NotificationClientAutoConfiguration {

    @Bean
    @ConditionalOnBean(name = ["notificationEntityManager"])
    @ConditionalOnMissingBean(NotificationPort::class)
    fun jpaNotificationAdapter(
        @Qualifier("notificationEntityManager") entityManager: EntityManager,
        objectMapper: ObjectMapper?
    ): NotificationPort {
        return JpaNotificationAdapter(entityManager, objectMapper ?: ObjectMapper())
    }

    @Bean
    @ConditionalOnMissingBean(NotificationPort::class)
    fun loggingNotificationAdapter(): NotificationPort {
        return LoggingNotificationAdapter()
    }
}
