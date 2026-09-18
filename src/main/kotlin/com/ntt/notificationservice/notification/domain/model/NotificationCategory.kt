package com.ntt.notificationservice.notification.domain.model

/**
 * Notification category for inbox classification.
 * Used by IN_APP channel to group notifications in the inbox UI.
 */
enum class NotificationCategory {
    SYSTEM,
    SOCIAL,
    TRANSACTIONAL,
    MARKETING
}
