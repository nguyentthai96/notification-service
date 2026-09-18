package com.ntt.notificationservice.notification.application

import com.ntt.notificationservice.notification.adapter.out.persistence.repository.NotificationQueueRepository
import com.ntt.notificationservice.notification.config.NotificationProperties
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Cleanup scheduler — removes old processed notifications daily.
 * Also handles inbox cleanup (archive old + enforce per-user limit).
 */
@Component
class NotificationCleanupScheduler(
    private val queueRepository: NotificationQueueRepository,
    private val properties: NotificationProperties,
    private val inboxService: InboxService
) {
    private val log = LoggerFactory.getLogger(NotificationCleanupScheduler::class.java)

    /**
     * Daily cleanup at 2:00 AM — delete old SENT/DELIVERED/READ notifications.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    fun cleanupOldNotifications() {
        val cutoff = Instant.now().minus(properties.queue.cleanupAfterDays, ChronoUnit.DAYS)
        val deleted = queueRepository.deleteProcessedBefore(cutoff)
        log.info("Cleanup: deleted {} old notifications (before {})", deleted, cutoff)
    }

    /**
     * Daily inbox cleanup at 3:00 AM — archive notifications older than retentionDays.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    fun cleanupInbox() {
        inboxService.cleanupOldNotifications()
    }
}

