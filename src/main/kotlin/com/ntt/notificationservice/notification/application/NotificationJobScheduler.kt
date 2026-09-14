package com.ntt.notificationservice.notification.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.ntt.notificationservice.notification.adapter.out.persistence.entity.NotificationDlqEntity
import com.ntt.notificationservice.notification.adapter.out.persistence.entity.NotificationRetryLogEntity
import com.ntt.notificationservice.notification.adapter.out.persistence.repository.NotificationDlqRepository
import com.ntt.notificationservice.notification.adapter.out.persistence.repository.NotificationQueueRepository
import com.ntt.notificationservice.notification.adapter.out.persistence.repository.NotificationRetryLogRepository
import com.ntt.notificationservice.notification.adapter.out.persistence.repository.NotificationTemplateRepository
import com.ntt.notificationservice.notification.config.NotificationProperties
import com.ntt.notificationservice.notification.domain.model.NotificationStatus
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Notification job scheduler — polls notification_queue and processes pending items.
 * Uses SELECT FOR UPDATE SKIP LOCKED for multi-instance safe processing.
 * Phase 2: Integrated with DLQ and retry log.
 */
@Component
class NotificationJobScheduler(
    private val queueRepository: NotificationQueueRepository,
    private val templateRepository: NotificationTemplateRepository,
    private val templateRenderService: TemplateRenderService,
    private val dispatcher: NotificationDispatcher,
    private val properties: NotificationProperties,
    private val objectMapper: ObjectMapper,
    private val dlqRepository: NotificationDlqRepository,
    private val retryLogRepository: NotificationRetryLogRepository,
    meterRegistry: MeterRegistry
) {
    private val log = LoggerFactory.getLogger(NotificationJobScheduler::class.java)

    private val sendSuccessCounter: Counter = Counter.builder("notification.send")
        .tag("result", "success")
        .register(meterRegistry)

    private val sendFailedCounter: Counter = Counter.builder("notification.send")
        .tag("result", "failed")
        .register(meterRegistry)

    private val sendRetryCounter: Counter = Counter.builder("notification.send")
        .tag("result", "retry")
        .register(meterRegistry)

    private val dlqCounter: Counter = Counter.builder("notification.dlq")
        .register(meterRegistry)

    /**
     * Poll and process pending notifications.
     */
    @Scheduled(fixedDelayString = "\${app.notification.queue.poll-interval-ms:5000}")
    @Transactional
    fun processQueue() {
        val now = Instant.now()
        val batch = queueRepository.findPendingForProcessing(now, properties.queue.batchSize)

        if (batch.isEmpty()) return

        log.debug("Processing {} pending notifications", batch.size)

        for (notification in batch) {
            notification.status = NotificationStatus.PROCESSING
            queueRepository.save(notification)

            try {
                // Render template if not already rendered
                if (notification.bodyRendered.isNullOrBlank()) {
                    val template = templateRepository.findByCodeAndActiveTrue(notification.templateCode)
                    if (template == null) {
                        notification.status = NotificationStatus.FAILED
                        notification.errorMessage = "Template not found: ${notification.templateCode}"
                        queueRepository.save(notification)
                        sendFailedCounter.increment()
                        log.warn("Template not found: code={}", notification.templateCode)
                        continue
                    }

                    val data: Map<String, Any> = objectMapper.readValue(notification.templateData)
                    notification.subject = templateRenderService.renderSubject(template, data)
                    notification.bodyRendered = templateRenderService.renderBody(template, data)
                }

                // Dispatch to channel sender
                dispatcher.dispatch(notification)

                // Success
                notification.status = NotificationStatus.SENT
                notification.sentAt = Instant.now()
                notification.errorMessage = null
                queueRepository.save(notification)
                sendSuccessCounter.increment()

                log.info(
                    "Notification sent: id={}, channel={}, recipient={}",
                    notification.id, notification.channel, notification.recipient
                )
            } catch (e: Exception) {
                handleFailure(notification, e)
            }
        }
    }

    /**
     * Handle delivery failure — retry with exponential backoff or move to DLQ.
     */
    private fun handleFailure(notification: com.ntt.notificationservice.notification.adapter.out.persistence.entity.NotificationQueueEntity, e: Exception) {
        notification.retryCount++
        notification.errorMessage = e.message?.take(2000)

        // Log every retry attempt (FR-014)
        val retryLog = NotificationRetryLogEntity().apply {
            notificationId = notification.id!!
            attemptNumber = notification.retryCount
            errorCode = extractErrorCode(e)
            errorMessage = e.message?.take(2000)
            attemptedAt = Instant.now()
        }
        retryLogRepository.save(retryLog)

        if (notification.retryCount >= notification.maxRetries) {
            notification.status = NotificationStatus.FAILED
            sendFailedCounter.increment()

            // Move to DLQ (FR-013)
            val dlqEntry = NotificationDlqEntity().apply {
                notificationId = notification.id!!
                channel = notification.channel.name
                errorCode = extractErrorCode(e)
                errorMessage = e.message?.take(2000)
                originalPayload = objectMapper.writeValueAsString(mapOf(
                    "recipient" to notification.recipient,
                    "templateCode" to notification.templateCode,
                    "templateData" to notification.templateData,
                    "channel" to notification.channel.name,
                    "subChannel" to notification.subChannel
                ))
                resolved = false
            }
            dlqRepository.save(dlqEntry)
            dlqCounter.increment()

            log.error(
                "Notification moved to DLQ after {} retries: id={}, error={}",
                notification.retryCount, notification.id, e.message
            )
        } else {
            // Exponential backoff: base * 2^retryCount
            val delaySeconds = properties.queue.baseRetryDelaySeconds *
                (1L shl notification.retryCount)
            notification.status = NotificationStatus.PENDING
            notification.nextRetryAt = Instant.now().plusSeconds(delaySeconds)
            sendRetryCounter.increment()
            log.warn(
                "Notification retry scheduled: id={}, attempt={}/{}, nextRetryAt={}",
                notification.id, notification.retryCount, notification.maxRetries, notification.nextRetryAt
            )
        }

        queueRepository.save(notification)
    }

    private fun extractErrorCode(e: Exception): String? {
        return if (e is com.ntt.notificationservice.shared.exception.NotificationException) {
            e.errorCode.toErrorCodeBase().getCode()
        } else null
    }
}
