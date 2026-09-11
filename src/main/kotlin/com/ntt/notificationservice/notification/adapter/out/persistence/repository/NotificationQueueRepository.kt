package com.ntt.notificationservice.notification.adapter.out.persistence.repository

import com.ntt.notificationservice.notification.adapter.out.persistence.entity.NotificationQueueEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

/**
 * Repository for notification queue — supports polling with pessimistic locking.
 * Uses SELECT FOR UPDATE SKIP LOCKED for multi-instance safe processing.
 */
interface NotificationQueueRepository : JpaRepository<NotificationQueueEntity, Long> {

    /**
     * Find pending notifications ready for processing.
     * Priority ordering: URGENT → HIGH → NORMAL → LOW, then by created_at ASC.
     * Uses SKIP LOCKED to prevent concurrent processing across instances.
     */
    @Query(
        value = """
            SELECT * FROM notification_queue 
            WHERE status = 'PENDING' 
              AND (next_retry_at IS NULL OR next_retry_at <= :now) 
            ORDER BY 
              CASE priority 
                WHEN 'URGENT' THEN 0 
                WHEN 'HIGH' THEN 1 
                WHEN 'NORMAL' THEN 2 
                WHEN 'LOW' THEN 3 
              END, 
              created_at ASC 
            LIMIT :batchSize 
            FOR UPDATE SKIP LOCKED
        """,
        nativeQuery = true
    )
    fun findPendingForProcessing(
        @Param("now") now: Instant,
        @Param("batchSize") batchSize: Int
    ): List<NotificationQueueEntity>

    /**
     * Delete old processed notifications for cleanup.
     */
    @Modifying
    @Query("DELETE FROM NotificationQueueEntity n WHERE n.status IN (com.ntt.notificationservice.notification.domain.model.NotificationStatus.SENT, com.ntt.notificationservice.notification.domain.model.NotificationStatus.DELIVERED, com.ntt.notificationservice.notification.domain.model.NotificationStatus.READ) AND n.sentAt < :cutoff")
    fun deleteOldProcessedNotifications(@Param("cutoff") cutoff: Instant): Int

    /**
     * Find by correlation ID for dedup check.
     */
    fun findByCorrelationId(correlationId: String): NotificationQueueEntity?
}
