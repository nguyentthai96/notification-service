package com.ntt.notificationservice.notification.adapter.out.persistence.repository

import com.ntt.notificationservice.notification.adapter.out.persistence.entity.NotificationQueueEntity
import com.ntt.notificationservice.notification.domain.model.NotificationChannel
import com.ntt.notificationservice.notification.domain.model.NotificationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * Repository for notification queue entities.
 * Supports scheduled polling with SELECT FOR UPDATE SKIP LOCKED.
 */
@Repository
interface NotificationQueueRepository : JpaRepository<NotificationQueueEntity, Long> {

    fun findByCorrelationId(correlationId: String): NotificationQueueEntity?

    fun findByExternalMessageId(externalMessageId: String): NotificationQueueEntity?

    @Query(
        value = """
            SELECT * FROM notification_queue
            WHERE status = 'PENDING'
            AND (next_retry_at IS NULL OR next_retry_at <= :now)
            ORDER BY
                CASE priority WHEN 'HIGH' THEN 1 WHEN 'NORMAL' THEN 2 WHEN 'LOW' THEN 3 END,
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

    fun countByRecipientAndTemplateCodeAndChannelAndCreatedAtAfter(
        recipient: String,
        templateCode: String,
        channel: NotificationChannel,
        createdAt: Instant
    ): Long

    fun findByIdAndStatus(id: Long, status: NotificationStatus): NotificationQueueEntity?

    @Query(
        value = """
            SELECT channel,
                   status,
                   COUNT(*) as cnt
            FROM notification_queue
            WHERE created_at BETWEEN :fromDate AND :toDate
            AND (:channel IS NULL OR channel = CAST(:channel AS VARCHAR))
            GROUP BY channel, status
            ORDER BY channel, status
        """,
        nativeQuery = true
    )
    fun findStatsByChannelAndDateRange(
        @Param("fromDate") fromDate: Instant,
        @Param("toDate") toDate: Instant,
        @Param("channel") channel: String?
    ): List<Array<Any>>

    @Query(
        value = """
            SELECT channel,
                   SUM(retry_count) as total_retries,
                   COUNT(CASE WHEN status = 'FAILED' THEN 1 END) as failed_count,
                   error_message
            FROM notification_queue
            WHERE created_at BETWEEN :fromDate AND :toDate
            AND retry_count > 0
            GROUP BY channel, error_message
            ORDER BY total_retries DESC
        """,
        nativeQuery = true
    )
    fun findRetryStatsByDateRange(
        @Param("fromDate") fromDate: Instant,
        @Param("toDate") toDate: Instant
    ): List<Array<Any>>

    @Query(
        value = """
            DELETE FROM notification_queue
            WHERE status IN ('SENT', 'DELIVERED', 'READ', 'REVOKED')
            AND updated_at < :cutoff
        """,
        nativeQuery = true
    )
    fun deleteProcessedBefore(@Param("cutoff") cutoff: Instant): Int
}
