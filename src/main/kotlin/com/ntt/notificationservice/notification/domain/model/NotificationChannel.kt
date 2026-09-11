package com.ntt.notificationservice.notification.domain.model

/**
 * Notification delivery channels.
 * Phase 1: EMAIL only. Others enabled via configuration.
 */
enum class NotificationChannel {
    EMAIL,
    SMS,
    PUSH,
    OTT
}
