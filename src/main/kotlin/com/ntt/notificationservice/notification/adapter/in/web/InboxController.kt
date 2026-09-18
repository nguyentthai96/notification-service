package com.ntt.notificationservice.notification.adapter.`in`.web

import com.ntt.notificationservice.notification.adapter.`in`.web.dto.InboxPageResponse
import com.ntt.notificationservice.notification.adapter.`in`.web.dto.UnreadCountResponse
import com.ntt.notificationservice.notification.application.InboxService
import com.ntt.notificationservice.notification.domain.model.NotificationCategory
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Inbox REST API controller.
 * Endpoints: list, unread-count, mark-read, mark-all-read, archive.
 * User identity provided by gateway via X-User-Id header.
 */
@RestController
@RequestMapping("/api/v1/inbox")
class InboxController(
    private val inboxService: InboxService
) {

    /**
     * Get paginated inbox list.
     * GET /api/v1/inbox?page=0&size=20&category=SYSTEM&read=false
     */
    @GetMapping
    fun getInbox(
        @RequestHeader("X-User-Id") userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) category: NotificationCategory?,
        @RequestParam(required = false) read: Boolean?
    ): ResponseEntity<InboxPageResponse> {
        val pageable = PageRequest.of(page, size.coerceIn(1, 100))
        val result = inboxService.getInbox(userId, category, read, pageable)
        return ResponseEntity.ok(result)
    }

    /**
     * Get unread notification count (badge counter).
     * GET /api/v1/inbox/unread-count
     */
    @GetMapping("/unread-count")
    fun getUnreadCount(
        @RequestHeader("X-User-Id") userId: Long
    ): ResponseEntity<UnreadCountResponse> {
        val count = inboxService.getUnreadCount(userId)
        return ResponseEntity.ok(UnreadCountResponse(unreadCount = count))
    }

    /**
     * Mark a single notification as read.
     * PATCH /api/v1/inbox/{id}/read
     */
    @PatchMapping("/{id}/read")
    fun markAsRead(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        inboxService.markAsRead(userId, id)
        return ResponseEntity.noContent().build()
    }

    /**
     * Mark all notifications as read.
     * PATCH /api/v1/inbox/read-all
     */
    @PatchMapping("/read-all")
    fun markAllAsRead(
        @RequestHeader("X-User-Id") userId: Long
    ): ResponseEntity<Void> {
        inboxService.markAllAsRead(userId)
        return ResponseEntity.noContent().build()
    }

    /**
     * Archive (soft-delete) a notification.
     * DELETE /api/v1/inbox/{id}
     */
    @DeleteMapping("/{id}")
    fun archive(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        inboxService.archive(userId, id)
        return ResponseEntity.noContent().build()
    }
}
