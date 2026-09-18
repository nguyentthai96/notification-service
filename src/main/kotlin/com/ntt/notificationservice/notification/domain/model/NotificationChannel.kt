package com.ntt.notificationservice.notification.domain.model

/**
 * Notification delivery channels.
 * Phase 1: EMAIL only. Others enabled via configuration.
 * IN_APP: In-app real-time notification via SSE + inbox.
 */
enum class NotificationChannel {
    EMAIL,
    SMS,
    PUSH,
    OTT,
    IN_APP
}
