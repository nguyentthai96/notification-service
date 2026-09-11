package com.ntt.notificationservice.notification.adapter.out.persistence.repository

import com.ntt.notificationservice.notification.adapter.out.persistence.entity.NotificationTemplateEntity
import org.springframework.data.jpa.repository.JpaRepository

/**
 * Repository for notification templates.
 */
interface NotificationTemplateRepository : JpaRepository<NotificationTemplateEntity, Long> {

    /**
     * Find active template by code.
     */
    fun findByCodeAndActiveTrue(code: String): NotificationTemplateEntity?
}
