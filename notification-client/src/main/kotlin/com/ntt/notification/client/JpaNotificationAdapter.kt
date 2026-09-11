package com.ntt.notification.client

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * JPA implementation of NotificationPort.
 * Directly inserts into notification_queue table via native SQL —
 * shares the same transaction as the calling business operation (Transactional Outbox).
 *
 * Requires secondary datasource pointing to notification-db.
 */
@Component
class JpaNotificationAdapter(
    @PersistenceContext(unitName = "notificationEntityManager")
    private val entityManager: EntityManager,
    private val objectMapper: ObjectMapper
) : NotificationPort {

    private val log = LoggerFactory.getLogger(JpaNotificationAdapter::class.java)

    @Transactional
    override fun enqueue(request: NotificationRequest): Long {
        val templateDataJson = objectMapper.writeValueAsString(request.templateData)

        // Use native SQL to insert into notification_queue — avoids needing the entity class
        val id = entityManager.createNativeQuery(
            """
            INSERT INTO notification_queue 
                (id, correlation_id, channel, priority, recipient, subject, template_code, template_data, 
                 status, retry_count, max_retries, source_service, created_at, updated_at)
            VALUES 
                (nextval('notification_queue_id_seq'), :correlationId, :channel, :priority, :recipient, NULL, 
                 :templateCode, CAST(:templateData AS jsonb), 'PENDING', 0, 3, :sourceService, NOW(), NOW())
            RETURNING id
            """
        )
            .setParameter("correlationId", request.correlationId)
            .setParameter("channel", request.channel)
            .setParameter("priority", request.priority)
            .setParameter("recipient", request.recipient)
            .setParameter("templateCode", request.templateCode)
            .setParameter("templateData", templateDataJson)
            .setParameter("sourceService", request.sourceService)
            .singleResult as Long

        log.info(
            "Notification enqueued via client: id={}, channel={}, recipient={}",
            id, request.channel, request.recipient
        )

        return id
    }
}
