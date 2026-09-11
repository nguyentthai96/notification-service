package com.ntt.notification.client

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.ComponentScan

/**
 * Auto-configuration for notification-client.
 * Activated when `app.notification.client.enabled=true` (default: true).
 * Scans this package for @Component beans (JpaNotificationAdapter).
 */
@AutoConfiguration
@ConditionalOnProperty(
    prefix = "app.notification.client",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@ComponentScan(basePackageClasses = [NotificationClientAutoConfiguration::class])
class NotificationClientAutoConfiguration
