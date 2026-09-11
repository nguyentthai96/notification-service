package com.ntt.notificationservice.notification.domain.model

/**
 * Notification priority — affects processing order.
 * URGENT processed first, LOW last (ORDER BY in SKIP LOCKED query).
 */
enum class NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}
