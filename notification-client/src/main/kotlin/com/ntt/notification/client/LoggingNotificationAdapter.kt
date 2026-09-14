package com.ntt.notification.client

import org.slf4j.LoggerFactory

/**
 * Fallback logging implementation of NotificationPort.
 * Used when no specific transport adapter (JPA outbox, Kafka, gRPC) is configured (e.g. in local development).
 */
class LoggingNotificationAdapter : NotificationPort {

    private val log = LoggerFactory.getLogger(LoggingNotificationAdapter::class.java)

    override fun enqueue(request: NotificationRequest): Long {
        val simulatedId = System.currentTimeMillis()
        log.info(
            "Notification enqueued via logging adapter: id={}, channel={}, recipient={}, templateCode={}, sourceService={}",
            simulatedId, request.channel, request.recipient, request.templateCode, request.sourceService
        )
        return simulatedId
    }
}
