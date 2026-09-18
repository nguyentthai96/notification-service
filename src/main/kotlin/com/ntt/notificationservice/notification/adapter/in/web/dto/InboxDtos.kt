package com.ntt.notificationservice.notification.adapter.`in`.web.dto

import java.time.Instant

/**
 * DTOs for notification inbox REST API.
 */

/** Single inbox notification — public-facing fields only. */
data class InboxNotificationResponse(
    val id: Long,
    val category: String,
    val title: String,
    val body: String,
    val iconUrl: String?,
    val actionUrl: String?,
    val priority: String,
    val metadata: String,
    val read: Boolean,
    val readAt: Instant?,
    val sourceService: String?,
    val createdAt: Instant?
)

/** Paginated inbox response with embedded unread count. */
data class InboxPageResponse(
    val content: List<InboxNotificationResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
    val size: Int,
    val unreadCount: Long
)

/** Badge counter response. */
data class UnreadCountResponse(
    val unreadCount: Long
)

/** SSE ticket response. */
data class SseTicketResponse(
    val ticket: String,
    val expiresIn: Int
)
