package com.ntt.notificationservice.notification.adapter.out.persistence.entity

import com.ntt.basecore.model.id.SnowflakePersistentAuditableEntity
import com.ntt.notificationservice.notification.domain.model.NotificationChannel
import com.ntt.notificationservice.notification.domain.model.NotificationPriority
import com.ntt.notificationservice.notification.domain.model.NotificationStatus
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

/**
 * Notification queue entity — transactional outbox for async notification delivery.
 * Records are created within the same @Transactional as the business operation,
 * then processed asynchronously by NotificationJobScheduler.
 *
 * Extended from auth-service MailQueueEntity with multi-channel support.
 */
@Entity
@Table(name = "notification_queue")
class NotificationQueueEntity : SnowflakePersistentAuditableEntity() {

    @Column(name = "correlation_id", length = 100, unique = true)
    var correlationId: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    lateinit var channel: NotificationChannel

    @Column(name = "sub_channel", length = 20)
    var subChannel: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    var priority: NotificationPriority = NotificationPriority.NORMAL

    @Column(name = "recipient", nullable = false, length = 255)
    lateinit var recipient: String

    @Column(name = "subject", length = 500)
    var subject: String? = null

    @Column(name = "template_code", nullable = false, length = 100)
    lateinit var templateCode: String

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "template_data", nullable = false, columnDefinition = "jsonb")
    var templateData: String = "{}"

    @Column(name = "body_rendered", columnDefinition = "TEXT")
    var bodyRendered: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: NotificationStatus = NotificationStatus.PENDING

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0

    @Column(name = "max_retries", nullable = false)
    var maxRetries: Int = 3

    @Column(name = "error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null

    @Column(name = "sent_at")
    var sentAt: Instant? = null

    @Column(name = "delivered_at")
    var deliveredAt: Instant? = null

    @Column(name = "read_at")
    var readAt: Instant? = null

    @Column(name = "revoked_at")
    var revokedAt: Instant? = null

    @Column(name = "revoke_reason", length = 500)
    var revokeReason: String? = null

    @Column(name = "next_retry_at")
    var nextRetryAt: Instant? = null

    @Column(name = "source_service", length = 100)
    var sourceService: String? = null

    @Column(name = "external_message_id", length = 255)
    var externalMessageId: String? = null
}
