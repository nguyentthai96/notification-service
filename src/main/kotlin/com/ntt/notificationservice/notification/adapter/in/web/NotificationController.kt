package com.ntt.notificationservice.notification.adapter.`in`.web

import com.ntt.notificationservice.notification.adapter.`in`.web.dto.EnqueueNotificationRequest
import com.ntt.notificationservice.notification.adapter.`in`.web.dto.EnqueueNotificationResponse
import com.ntt.notificationservice.notification.adapter.`in`.web.dto.NotificationStatusResponse
import com.ntt.notificationservice.notification.adapter.`in`.web.dto.RevokeRequest
import com.ntt.notificationservice.notification.adapter.out.persistence.repository.NotificationQueueRepository
import com.ntt.notificationservice.notification.application.NotificationEnqueueService
import com.ntt.notificationservice.notification.application.NotificationRevokeService
import com.ntt.notificationservice.notification.domain.model.NotificationChannel
import com.ntt.notificationservice.notification.domain.model.NotificationPriority
import com.ntt.notificationservice.shared.exception.NotificationErrorCode
import com.ntt.notificationservice.shared.exception.NotificationException
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Notification REST API controller.
 * Endpoints: enqueue, status, retry, revoke, list.
 */
@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    private val enqueueService: NotificationEnqueueService,
    private val revokeService: NotificationRevokeService,
    private val queueRepository: NotificationQueueRepository
) {

    /**
     * Enqueue a new notification.
     * POST /api/v1/notifications
     */
    @PostMapping
    fun enqueue(@Valid @RequestBody request: EnqueueNotificationRequest): ResponseEntity<EnqueueNotificationResponse> {
        val channel = try {
            NotificationChannel.valueOf(request.channel.uppercase())
        } catch (e: IllegalArgumentException) {
            throw NotificationException(
                NotificationErrorCode.CHANNEL_NOT_SUPPORTED,
                "Invalid channel: ${request.channel}"
            )
        }

        val priority = try {
            NotificationPriority.valueOf(request.priority.uppercase())
        } catch (e: IllegalArgumentException) {
            NotificationPriority.NORMAL
        }

        val id = enqueueService.enqueue(
            recipient = request.recipient,
            templateCode = request.templateCode,
            templateData = request.templateData,
            channel = channel,
            priority = priority,
            correlationId = request.correlationId,
            sourceService = request.sourceService,
            createdBy = request.createdBy
        )

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(EnqueueNotificationResponse(id = id))
    }

    /**
     * Get notification status by ID.
     * GET /api/v1/notifications/{id}
     */
    @GetMapping("/{id}")
    fun getStatus(@PathVariable id: Long): ResponseEntity<NotificationStatusResponse> {
        val notification = queueRepository.findById(id)
            .orElseThrow { NotificationException(NotificationErrorCode.NOT_FOUND) }

        return ResponseEntity.ok(
            NotificationStatusResponse(
                id = notification.id!!,
                correlationId = notification.correlationId,
                channel = notification.channel.name,
                priority = notification.priority.name,
                recipient = notification.recipient,
                templateCode = notification.templateCode,
                status = notification.status.name,
                retryCount = notification.retryCount,
                maxRetries = notification.maxRetries,
                errorMessage = notification.errorMessage,
                sentAt = notification.sentAt,
                deliveredAt = notification.deliveredAt,
                readAt = notification.readAt,
                revokedAt = notification.revokedAt,
                revokeReason = notification.revokeReason,
                sourceService = notification.sourceService,
                createdAt = notification.createdAt
            )
        )
    }

    /**
     * Manual retry a FAILED notification.
     * POST /api/v1/notifications/{id}/retry
     */
    @PostMapping("/{id}/retry")
    fun retry(@PathVariable id: Long): ResponseEntity<Map<String, Any>> {
        revokeService.retry(id)
        return ResponseEntity.ok(mapOf("id" to id, "status" to "PENDING", "message" to "Retry scheduled"))
    }

    /**
     * Revoke or cancel a notification.
     * POST /api/v1/notifications/{id}/revoke
     */
    @PostMapping("/{id}/revoke")
    fun revoke(
        @PathVariable id: Long,
        @Valid @RequestBody request: RevokeRequest
    ): ResponseEntity<Map<String, Any>> {
        revokeService.revoke(id, request.reason)
        return ResponseEntity.ok(mapOf("id" to id, "message" to "Notification revoked"))
    }
}
