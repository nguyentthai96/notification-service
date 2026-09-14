package com.ntt.notificationservice.notification.adapter.`in`.kafka

import com.ntt.notificationservice.notification.application.NotificationEnqueueService
import com.ntt.notificationservice.notification.domain.model.NotificationChannel
import com.ntt.notificationservice.notification.domain.model.NotificationPriority
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

/**
 * Kafka consumer for notification-inbound topic.
 * Supports dual JSON/Protobuf deserialization.
 * Only active when app.notification.kafka.enabled = true.
 */
@Component
@ConditionalOnProperty(
    prefix = "app.notification.kafka",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class KafkaNotificationConsumer(
    private val enqueueService: NotificationEnqueueService
) {
    private val log = LoggerFactory.getLogger(KafkaNotificationConsumer::class.java)
    private val deserializer = SmartNotificationDeserializer()

    @KafkaListener(
        topics = ["notification-inbound"],
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun consume(record: ConsumerRecord<String, ByteArray>) {
        try {
            val contentType = record.headers()
                .lastHeader("content-type")
                ?.value()
                ?.let { String(it, StandardCharsets.UTF_8) }

            val request = deserializer.deserialize(record.value(), contentType)

            // Build correlationId for dedup (FR-021)
            val correlationId = request.correlationId
                ?: "${request.sourceService ?: "kafka"}:${record.key() ?: record.offset()}"

            val channel = NotificationChannel.valueOf(request.channel.uppercase())
            val priority = NotificationPriority.valueOf(request.priority.uppercase())

            enqueueService.enqueue(
                recipient = request.recipient,
                templateCode = request.templateCode,
                templateData = request.templateData,
                channel = channel,
                priority = priority,
                correlationId = correlationId,
                sourceService = request.sourceService,
                subChannel = request.subChannel
            )

            log.info(
                "Kafka notification consumed: topic={}, offset={}, correlationId={}",
                record.topic(), record.offset(), correlationId
            )

        } catch (e: Exception) {
            log.error("Kafka consumption failed: topic={}, offset={}, error={}",
                record.topic(), record.offset(), e.message)
            throw e  // Let DefaultErrorHandler route to DLT
        }
    }
}
