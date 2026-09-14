package com.ntt.notificationservice.notification.application

import com.ntt.notificationservice.notification.adapter.out.persistence.repository.NotificationQueueRepository
import com.ntt.notificationservice.notification.domain.model.NotificationStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Delivery tracking service — updates notification status based on webhook callbacks.
 */
@Service
class DeliveryTrackingService(
    private val queueRepository: NotificationQueueRepository
) {
    private val log = LoggerFactory.getLogger(DeliveryTrackingService::class.java)

    /**
     * Update delivery status from external webhook callback.
     */
    @Transactional
    fun updateDeliveryStatus(externalMessageId: String, newStatus: String, timestamp: Instant = Instant.now()) {
        val notification = queueRepository.findByExternalMessageId(externalMessageId) ?: run {
            log.warn("Notification not found for externalMessageId={}", externalMessageId)
            return
        }

        when (newStatus.uppercase()) {
            "DELIVERED", "DELIVERY_SUCCESS" -> {
                notification.status = NotificationStatus.DELIVERED
                notification.deliveredAt = timestamp
            }
            "READ" -> {
                notification.status = NotificationStatus.READ
                notification.readAt = timestamp
            }
            "FAILED", "UNDELIVERED" -> {
                notification.status = NotificationStatus.FAILED
                notification.errorMessage = "Delivery failed (provider callback: $newStatus)"
            }
            else -> {
                log.debug("Ignored webhook status: externalMessageId={}, status={}", externalMessageId, newStatus)
                return
            }
        }

        queueRepository.save(notification)
        log.info(
            "Delivery status updated: id={}, externalMessageId={}, status={}",
            notification.id, externalMessageId, notification.status
        )
    }
}
