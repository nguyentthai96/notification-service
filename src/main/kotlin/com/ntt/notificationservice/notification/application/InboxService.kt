package com.ntt.notificationservice.notification.application

import com.ntt.notificationservice.notification.adapter.`in`.web.dto.InboxNotificationResponse
import com.ntt.notificationservice.notification.adapter.`in`.web.dto.InboxPageResponse
import com.ntt.notificationservice.notification.adapter.out.persistence.entity.NotificationInboxEntity
import com.ntt.notificationservice.notification.adapter.out.persistence.repository.NotificationInboxRepository
import com.ntt.notificationservice.notification.config.NotificationProperties
import com.ntt.notificationservice.notification.domain.model.NotificationCategory
import com.ntt.notificationservice.shared.exception.NotificationErrorCode
import com.ntt.notificationservice.shared.exception.NotificationException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Inbox service — CRUD operations + badge cache for in-app notifications.
 * Badge count cached in Redis with 60s TTL to reduce DB load.
 */
@Service
class InboxService(
    private val inboxRepository: NotificationInboxRepository,
    private val redisTemplate: StringRedisTemplate,
    private val properties: NotificationProperties
) {
    private val log = LoggerFactory.getLogger(InboxService::class.java)

    companion object {
        private const val BADGE_KEY_PREFIX = "notification:badge:"
        private const val BADGE_TTL_SECONDS = 60L
    }

    /**
     * Get paginated inbox for user with optional filters.
     */
    @Transactional(readOnly = true)
    fun getInbox(
        userId: Long,
        category: NotificationCategory?,
        readStatus: Boolean?,
        pageable: Pageable
    ): InboxPageResponse {
        val page: Page<NotificationInboxEntity> = when {
            category != null -> inboxRepository.findByUserIdAndCategoryAndArchivedFalseOrderByCreatedAtDesc(
                userId, category, pageable
            )
            readStatus != null -> inboxRepository.findByUserIdAndReadAndArchivedFalseOrderByCreatedAtDesc(
                userId, readStatus, pageable
            )
            else -> inboxRepository.findByUserIdAndArchivedFalseOrderByCreatedAtDesc(userId, pageable)
        }

        val unreadCount = getUnreadCount(userId)

        return InboxPageResponse(
            content = page.content.map { it.toResponse() },
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            page = page.number,
            size = page.size,
            unreadCount = unreadCount
        )
    }

    /**
     * Get unread count with Redis cache.
     */
    fun getUnreadCount(userId: Long): Long {
        val key = "$BADGE_KEY_PREFIX$userId"
        val cached = redisTemplate.opsForValue().get(key)
        if (cached != null) {
            return cached.toLong()
        }

        val count = inboxRepository.countByUserIdAndReadFalseAndArchivedFalse(userId)
        redisTemplate.opsForValue().set(key, count.toString(), Duration.ofSeconds(BADGE_TTL_SECONDS))
        return count
    }

    /**
     * Mark a single notification as read. Validates ownership.
     */
    @Transactional
    fun markAsRead(userId: Long, notificationId: Long) {
        val entity = findOwnedNotification(userId, notificationId)

        if (!entity.read) {
            entity.read = true
            entity.readAt = Instant.now()
            inboxRepository.save(entity)
            invalidateBadgeCache(userId)
            log.debug("Notification marked as read: id={}, userId={}", notificationId, userId)
        }
    }

    /**
     * Mark all unread notifications as read for user. Single batch UPDATE.
     */
    @Transactional
    fun markAllAsRead(userId: Long) {
        val updated = inboxRepository.markAllAsRead(userId, Instant.now())
        if (updated > 0) {
            invalidateBadgeCache(userId)
            log.info("Marked {} notifications as read for userId={}", updated, userId)
        }
    }

    /**
     * Soft-delete (archive) a notification. Validates ownership.
     */
    @Transactional
    fun archive(userId: Long, notificationId: Long) {
        val entity = findOwnedNotification(userId, notificationId)

        entity.archived = true
        entity.archivedAt = Instant.now()
        inboxRepository.save(entity)
        invalidateBadgeCache(userId)
        log.debug("Notification archived: id={}, userId={}", notificationId, userId)
    }

    /**
     * Cleanup: archive notifications older than retentionDays.
     * Called by scheduled job.
     */
    @Transactional
    fun cleanupOldNotifications() {
        val retentionDays = properties.inApp.inbox.retentionDays
        val cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS)
        val archived = inboxRepository.archiveAllOlderThan(cutoff, Instant.now())
        if (archived > 0) {
            log.info("Inbox cleanup: archived {} notifications older than {} days", archived, retentionDays)
        }
    }

    /**
     * Invalidate badge cache for a user.
     */
    fun invalidateBadgeCache(userId: Long) {
        redisTemplate.delete("$BADGE_KEY_PREFIX$userId")
    }

    private fun findOwnedNotification(userId: Long, notificationId: Long): NotificationInboxEntity {
        val entity = inboxRepository.findByIdAndUserId(notificationId, userId)
            ?: throw NotificationException(NotificationErrorCode.INBOX_NOT_FOUND)

        if (entity.userId != userId) {
            throw NotificationException(NotificationErrorCode.INBOX_ACCESS_DENIED)
        }

        if (entity.archived) {
            throw NotificationException(NotificationErrorCode.INBOX_NOT_FOUND)
        }

        return entity
    }

    private fun NotificationInboxEntity.toResponse(): InboxNotificationResponse {
        return InboxNotificationResponse(
            id = this.id!!,
            category = this.category.name,
            title = this.title,
            body = this.body,
            iconUrl = this.iconUrl,
            actionUrl = this.actionUrl,
            priority = this.priority.name,
            metadata = this.metadata,
            read = this.read,
            readAt = this.readAt,
            sourceService = this.sourceService,
            createdAt = this.createdAt
        )
    }
}
