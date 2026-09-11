package com.ntt.notificationservice.notification.application

import com.ntt.notificationservice.notification.adapter.out.persistence.repository.NotificationQueueRepository
import com.ntt.notificationservice.notification.domain.model.NotificationStatus
import com.ntt.notificationservice.shared.exception.NotificationErrorCode
import com.ntt.notificationservice.shared.exception.NotificationException
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Notification revoke/cancel/retry service — status transition management.
 */
@Service
class NotificationRevokeService(
    private val queueRepository: NotificationQueueRepository,
    private val meterRegistry: MeterRegistry
) {
    private val log = LoggerFactory.getLogger(NotificationRevokeService::class.java)

    /**
     * Revoke or cancel a notification.
     * PENDING/PROCESSING → CANCELLED (pre-send)
     * SENT/DELIVERED/READ → REVOKED (logical revoke)
     */
    @Transactional
    fun revoke(notificationId: Long, reason: String?) {
        val notification = queueRepository.findById(notificationId)
            .orElseThrow { NotificationException(NotificationErrorCode.NOT_FOUND) }

        when (notification.status) {
            NotificationStatus.PENDING, NotificationStatus.PROCESSING -> {
                notification.status = NotificationStatus.CANCELLED
                notification.revokedAt = Instant.now()
                notification.revokeReason = reason
            }
            NotificationStatus.SENT, NotificationStatus.DELIVERED, NotificationStatus.READ -> {
                notification.status = NotificationStatus.REVOKED
                notification.revokedAt = Instant.now()
                notification.revokeReason = reason
            }
            else -> throw NotificationException(
                NotificationErrorCode.INVALID_STATUS_TRANSITION,
                "Cannot revoke notification in status: ${notification.status}"
            )
        }

        queueRepository.save(notification)

        meterRegistry.counter(
            "notification.revoke",
            "channel", notification.channel.name
        ).increment()

        log.info("Notification revoked: id={}, status={}", notificationId, notification.status)
    }

    /**
     * Manual retry a FAILED notification — reset to PENDING.
     */
    @Transactional
    fun retry(notificationId: Long) {
        val notification = queueRepository.findById(notificationId)
            .orElseThrow { NotificationException(NotificationErrorCode.NOT_FOUND) }

        if (notification.status != NotificationStatus.FAILED) {
            throw NotificationException(
                NotificationErrorCode.INVALID_STATUS_TRANSITION,
                "Can only retry FAILED notifications, current status: ${notification.status}"
            )
        }

        notification.status = NotificationStatus.PENDING
        notification.retryCount = 0
        notification.errorMessage = null
        notification.nextRetryAt = null
        queueRepository.save(notification)

        log.info("Notification retried: id={}", notificationId)
    }
}
