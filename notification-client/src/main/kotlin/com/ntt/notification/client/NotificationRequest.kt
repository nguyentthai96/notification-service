package com.ntt.notification.client

/**
 * Notification request — data class for enqueueing notifications.
 * Used by producer services via NotificationPort.
 */
data class NotificationRequest(
    val recipient: String,
    val templateCode: String,
    val templateData: Map<String, Any> = emptyMap(),
    val channel: String = "EMAIL",
    val priority: String = "NORMAL",
    val correlationId: String? = null,
    val sourceService: String? = null,
    val createdBy: Long? = null
)
