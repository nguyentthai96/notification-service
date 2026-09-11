package com.ntt.notificationservice.notification.adapter.out.persistence.entity

import com.ntt.basecore.model.id.SnowflakePersistentAuditableEntity
import com.ntt.notificationservice.notification.domain.model.NotificationChannel
import jakarta.persistence.*

/**
 * Notification template entity — stores templates with placeholder support.
 * Templates are referenced by code and rendered before delivery.
 */
@Entity
@Table(name = "notification_template")
class NotificationTemplateEntity : SnowflakePersistentAuditableEntity() {

    @Column(name = "code", nullable = false, unique = true, length = 100)
    lateinit var code: String

    @Column(name = "name", nullable = false, length = 255)
    lateinit var name: String

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    var channel: NotificationChannel = NotificationChannel.EMAIL

    @Column(name = "subject_template", nullable = false, length = 500)
    lateinit var subjectTemplate: String

    @Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
    lateinit var bodyTemplate: String

    @Column(name = "tracking_mode", nullable = false, length = 10)
    var trackingMode: String = "NONE"

    @Column(name = "language", nullable = false, length = 10)
    var language: String = "vi"

    @Column(name = "active", nullable = false)
    var active: Boolean = true
}
