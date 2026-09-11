package com.ntt.notificationservice.notification.domain.model

/**
 * Notification status lifecycle — 7 states + CANCELLED.
 *
 * Valid transitions:
 * - PENDING → PROCESSING (scheduler picks up)
 * - PROCESSING → SENT (delivery success)
 * - PROCESSING → FAILED (delivery error after max retries)
 * - PROCESSING → PENDING (retry with backoff)
 * - SENT → DELIVERED (webhook callback)
 * - DELIVERED → READ (read receipt)
 * - PENDING → CANCELLED (pre-send cancellation)
 * - SENT/DELIVERED/READ → REVOKED (logical revoke)
 *
 * Terminal states: FAILED, REVOKED, CANCELLED
 */
enum class NotificationStatus {
    PENDING,
    PROCESSING,
    SENT,
    DELIVERED,
    READ,
    FAILED,
    REVOKED,
    CANCELLED
}
