package com.ntt.notificationservice.notification.adapter.`in`.web.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

/**
 * DTOs for notification REST API.
 */

/** Request to enqueue a new notification. */
data class EnqueueNotificationRequest(
    @field:NotBlank(message = "recipient is required")
    @field:Size(max = 255, message = "recipient must not exceed 255 characters")
    val recipient: String,

    @field:NotBlank(message = "templateCode is required")
    @field:Size(max = 100, message = "templateCode must not exceed 100 characters")
    val templateCode: String,

    val templateData: Map<String, Any> = emptyMap(),

    @field:NotBlank(message = "channel is required")
    val channel: String = "EMAIL",

    val priority: String = "NORMAL",

    @field:Size(max = 100, message = "correlationId must not exceed 100 characters")
    val correlationId: String? = null,

    @field:Size(max = 100, message = "sourceService must not exceed 100 characters")
    val sourceService: String? = null,

    val createdBy: Long? = null,

    @field:Size(max = 20, message = "subChannel must not exceed 20 characters")
    val subChannel: String? = null
)

/** Response after enqueueing a notification. */
data class EnqueueNotificationResponse(
    val id: Long,
    val status: String = "PENDING"
)

/** Notification status response. */
data class NotificationStatusResponse(
    val id: Long,
    val correlationId: String?,
    val channel: String,
    val subChannel: String?,
    val priority: String,
    val recipient: String,
    val templateCode: String,
    val status: String,
    val retryCount: Int,
    val maxRetries: Int,
    val errorMessage: String?,
    val externalMessageId: String?,
    val sentAt: Instant?,
    val deliveredAt: Instant?,
    val readAt: Instant?,
    val revokedAt: Instant?,
    val revokeReason: String?,
    val sourceService: String?,
    val createdAt: Instant?
)

/** Request to revoke a notification. */
data class RevokeRequest(
    @field:Size(max = 500, message = "reason must not exceed 500 characters")
    val reason: String? = null
)
