package com.ntt.notificationservice.notification.adapter.`in`.web.admin

import com.ntt.notificationservice.notification.adapter.out.persistence.entity.NotificationQueueEntity
import com.ntt.notificationservice.notification.adapter.out.persistence.repository.NotificationDlqRepository
import com.ntt.notificationservice.notification.adapter.out.persistence.repository.NotificationQueueRepository
import com.ntt.notificationservice.notification.adapter.`in`.web.dto.DlqEntryResponse
import com.ntt.notificationservice.notification.domain.model.NotificationStatus
import com.ntt.notificationservice.shared.exception.NotificationErrorCode
import com.ntt.notificationservice.shared.exception.NotificationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

/**
 * Admin API for DLQ management.
 */
@RestController
@RequestMapping("/api/v1/notifications/dlq")
class DlqController(
    private val dlqRepository: NotificationDlqRepository,
    private val queueRepository: NotificationQueueRepository
) {

    @GetMapping
    fun list(pageable: Pageable): ResponseEntity<Page<DlqEntryResponse>> {
        val page = dlqRepository.findByResolved(false, pageable)
        return ResponseEntity.ok(page.map { it.toResponse() })
    }

    @PostMapping("/{id}/retry")
    fun retry(@PathVariable id: Long): ResponseEntity<Map<String, Any>> {
        val dlqEntry = dlqRepository.findById(id).orElseThrow {
            NotificationException(NotificationErrorCode.DLQ_ENTRY_NOT_FOUND, "DLQ entry not found: id=$id")
        }

        if (dlqEntry.resolved) {
            return ResponseEntity.badRequest().body(mapOf("error" to "DLQ entry already resolved"))
        }

        // Re-enqueue the notification for retry
        val notification = queueRepository.findById(dlqEntry.notificationId).orElse(null)
        if (notification != null) {
            notification.status = NotificationStatus.PENDING
            notification.retryCount = 0
            notification.nextRetryAt = null
            notification.errorMessage = null
            queueRepository.save(notification)
        }

        // Mark DLQ entry as resolved
        dlqEntry.resolved = true
        dlqEntry.resolvedAt = Instant.now()
        dlqRepository.save(dlqEntry)

        return ResponseEntity.ok(mapOf("message" to "DLQ entry re-enqueued for retry", "notificationId" to dlqEntry.notificationId))
    }

    @PostMapping("/{id}/discard")
    fun discard(@PathVariable id: Long): ResponseEntity<Map<String, Any>> {
        val dlqEntry = dlqRepository.findById(id).orElseThrow {
            NotificationException(NotificationErrorCode.DLQ_ENTRY_NOT_FOUND, "DLQ entry not found: id=$id")
        }

        dlqEntry.resolved = true
        dlqEntry.resolvedAt = Instant.now()
        dlqRepository.save(dlqEntry)

        return ResponseEntity.ok(mapOf("message" to "DLQ entry discarded", "notificationId" to dlqEntry.notificationId))
    }

    private fun com.ntt.notificationservice.notification.adapter.out.persistence.entity.NotificationDlqEntity.toResponse() = DlqEntryResponse(
        id = id!!,
        notificationId = notificationId,
        channel = channel,
        errorCode = errorCode,
        errorMessage = errorMessage,
        resolved = resolved,
        resolvedAt = resolvedAt,
        createdAt = createdAt
    )
}
