package com.ntt.notificationservice.notification.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.ntt.notificationservice.notification.adapter.out.persistence.entity.NotificationQueueEntity
import com.ntt.notificationservice.notification.adapter.out.persistence.repository.NotificationQueueRepository
import com.ntt.notificationservice.notification.domain.model.NotificationChannel
import com.ntt.notificationservice.notification.domain.model.NotificationPriority
import com.ntt.notificationservice.notification.domain.model.NotificationStatus
import com.ntt.notificationservice.notification.config.NotificationProperties
import com.ntt.notificationservice.shared.exception.NotificationErrorCode
import com.ntt.notificationservice.shared.exception.NotificationException
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Notification enqueue service — validates and inserts into notification_queue.
 * Called by REST API and notification-client SDK.
 */
@Service
class NotificationEnqueueService(
    private val queueRepository: NotificationQueueRepository,
    private val objectMapper: ObjectMapper,
    private val properties: NotificationProperties,
    private val meterRegistry: MeterRegistry
) {
    private val log = LoggerFactory.getLogger(NotificationEnqueueService::class.java)

    /**
     * Enqueue a new notification.
     * @return notification ID (Snowflake)
     */
    @Transactional
    fun enqueue(
        recipient: String,
        templateCode: String,
        templateData: Map<String, Any> = emptyMap(),
        channel: NotificationChannel = NotificationChannel.EMAIL,
        priority: NotificationPriority = NotificationPriority.NORMAL,
        correlationId: String? = null,
        sourceService: String? = null,
        createdBy: Long? = null
    ): Long {
        // Dedup check
        if (!correlationId.isNullOrBlank()) {
            val existing = queueRepository.findByCorrelationId(correlationId)
            if (existing != null) {
                throw NotificationException(
                    NotificationErrorCode.DUPLICATE,
                    "Duplicate notification: correlationId=$correlationId"
                )
            }
        }

        val entity = NotificationQueueEntity().apply {
            this.correlationId = correlationId
            this.channel = channel
            this.priority = priority
            this.recipient = recipient
            this.templateCode = templateCode
            this.templateData = objectMapper.writeValueAsString(templateData)
            this.status = NotificationStatus.PENDING
            this.maxRetries = properties.queue.maxRetries
            this.sourceService = sourceService
        }

        val saved = queueRepository.save(entity)

        meterRegistry.counter(
            "notification.enqueue",
            "source_service", sourceService ?: "unknown",
            "channel", channel.name
        ).increment()

        log.info(
            "Notification enqueued: id={}, channel={}, recipient={}, templateCode={}",
            saved.id, channel, recipient, templateCode
        )

        return saved.id!!
    }
}
